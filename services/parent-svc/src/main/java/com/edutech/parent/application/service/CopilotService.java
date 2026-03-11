package com.edutech.parent.application.service;

import com.edutech.parent.application.dto.ConversationResponse;
import com.edutech.parent.application.dto.MessageResponse;
import com.edutech.parent.application.exception.ConversationNotFoundException;
import com.edutech.parent.domain.model.CopilotConversation;
import com.edutech.parent.domain.model.StudentLink;
import com.edutech.parent.domain.port.out.CopilotConversationRepository;
import com.edutech.parent.domain.port.out.StudentLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates Parent Copilot conversations.
 *
 * Calls ai-gateway-svc via WebClient to get LLM completions.
 * Conversation history is persisted in CopilotConversation (JPA entity).
 * Graceful degradation: if AI is unavailable the service still saves the
 * user message and returns an appropriate fallback answer.
 */
@Service
@Transactional
public class CopilotService {

    private static final Logger log = LoggerFactory.getLogger(CopilotService.class);
    private static final String AI_UNAVAILABLE_REPLY =
            "I am currently unable to connect to the AI assistant. Please try again shortly.";
    private static final String COPILOT_TYPE = "PARENT_COPILOT";

    private final CopilotConversationRepository conversationRepository;
    private final StudentLinkRepository studentLinkRepository;
    private final WebClient aiGatewayWebClient;
    private final WebClient psychSvcWebClient;
    private final int timeoutSeconds;

    public CopilotService(
            CopilotConversationRepository conversationRepository,
            StudentLinkRepository studentLinkRepository,
            @Qualifier("aiGatewayWebClient") WebClient aiGatewayWebClient,
            @Qualifier("psychSvcWebClient") WebClient psychSvcWebClient,
            @Value("${ai-gateway.timeout-seconds:30}") int timeoutSeconds) {
        this.conversationRepository = conversationRepository;
        this.studentLinkRepository = studentLinkRepository;
        this.aiGatewayWebClient = aiGatewayWebClient;
        this.psychSvcWebClient = psychSvcWebClient;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Start a new conversation with an initial user message.
     *
     * @param parentId       the authenticated parent's user ID (as string)
     * @param studentId      optional studentId for context
     * @param initialMessage the first message from the parent
     * @return the saved conversation with both user and assistant messages
     */
    public ConversationResponse startConversation(String parentId, String studentId, String initialMessage) {
        String title = buildTitle(initialMessage);
        CopilotConversation conversation = new CopilotConversation(parentId, studentId, title);
        conversation.addMessage("user", initialMessage);

        String studentName = resolveStudentName(parentId, studentId);
        String psychContext = studentId != null ? fetchStudentPsychContext(studentId) : null;
        String aiReply = callAiGateway(initialMessage, List.of(), psychContext, studentName);
        conversation.addMessage("assistant", aiReply);

        CopilotConversation saved = conversationRepository.save(conversation);
        log.info("CopilotConversation started: id={} parentId={}", saved.getId(), parentId);
        return toResponse(saved);
    }

    /**
     * Continue an existing conversation with a new user message.
     *
     * @param conversationId the ID of the existing conversation
     * @param parentId       the authenticated parent's user ID — used to validate ownership
     * @param userMessage    the new message from the parent
     * @return the updated conversation
     */
    public ConversationResponse continueConversation(Long conversationId, String parentId, String userMessage) {
        CopilotConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        validateOwnership(conversation, parentId);

        // Build history from existing messages FIRST — triggers lazy load of the
        // PersistentList before any transient CopilotMessage is added.  If we
        // addMessage() before the collection is initialised, Hibernate auto-flushes
        // before the lazy SELECT and cannot cascade-persist the transient child.
        List<Map<String, String>> history = buildHistory(conversation);

        conversation.addMessage("user", userMessage);
        String studentName = resolveStudentName(parentId, conversation.getStudentId());
        String psychContext = conversation.getStudentId() != null
                ? fetchStudentPsychContext(conversation.getStudentId()) : null;
        String aiReply = callAiGateway(userMessage, history, psychContext, studentName);
        conversation.addMessage("assistant", aiReply);

        CopilotConversation saved = conversationRepository.save(conversation);
        log.info("CopilotConversation continued: id={} messages={}", saved.getId(), saved.getMessages().size());
        return toResponse(saved);
    }

    /**
     * Retrieve a conversation and all its messages.
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Long conversationId, String parentId) {
        CopilotConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        validateOwnership(conversation, parentId);
        return toResponse(conversation);
    }

    /**
     * Close a conversation, preventing further messages.
     */
    public void closeConversation(Long conversationId, String parentId) {
        CopilotConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        validateOwnership(conversation, parentId);
        conversation.close();
        conversationRepository.save(conversation);
        log.info("CopilotConversation closed: id={}", conversationId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String resolveStudentName(String parentId, String studentId) {
        if (parentId == null || studentId == null) return null;
        try {
            UUID parentUuid = UUID.fromString(parentId);
            UUID studentUuid = UUID.fromString(studentId);
            return studentLinkRepository.findByParentIdAndStudentId(parentUuid, studentUuid)
                    .map(StudentLink::getStudentName)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Could not resolve student name for parent={} student={}: {}", parentId, studentId, e.getMessage());
            return null;
        }
    }

    private String callAiGateway(String message, List<Map<String, String>> history, String studentContext, String studentName) {
        String historyText = history.stream()
                .map(m -> m.get("role") + ": " + m.get("content"))
                .collect(Collectors.joining("\n"));

        StringBuilder systemPrompt = new StringBuilder(
                "You are the NexusEd Parent Copilot. Help parents understand their child's academic progress, " +
                "psychometric profile, career recommendations, fees, attendance, weak areas, and exam schedules. " +
                "Be concise and supportive. IMPORTANT: Only use data explicitly provided in this prompt. " +
                "Do NOT fabricate, invent, or assume any details about the student that are not stated here.");

        if (studentName != null && !studentName.isBlank()) {
            systemPrompt.append("\n\nThe parent's linked child is: ").append(studentName).append(". ")
                    .append("Always refer to them by name. Do not invent any other details about them beyond what is given.");
        }

        if (studentContext != null && !studentContext.isBlank()) {
            systemPrompt.append("\n\n").append(studentContext);
        }

        if (!historyText.isBlank()) {
            systemPrompt.append("\n\nConversation history:\n").append(historyText);
        }

        Map<String, Object> requestBody = Map.of(
                "requesterId", "parent-copilot",
                "systemPrompt", systemPrompt.toString(),
                "userMessage", message,
                "maxTokens", 512,
                "temperature", 0.7
        );
        try {
            Map<?, ?> response = aiGatewayWebClient.post()
                    .uri("/api/v1/ai/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .onErrorReturn(Map.of())
                    .block(Duration.ofSeconds(timeoutSeconds));

            if (response != null && response.containsKey("content")) {
                String content = (String) response.get("content");
                return (content != null && !content.isBlank()) ? content : AI_UNAVAILABLE_REPLY;
            }
            return AI_UNAVAILABLE_REPLY;
        } catch (Exception e) {
            log.warn("AI gateway call failed for Parent Copilot: {}", e.getMessage());
            return AI_UNAVAILABLE_REPLY;
        }
    }

    private List<Map<String, String>> buildHistory(CopilotConversation conversation) {
        return conversation.getMessages().stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList());
    }

    /**
     * Calls psych-svc via X-Service-Key to fetch the student's psychometric profile
     * and formats it as a plain-English context block for the AI system prompt.
     */
    private String fetchStudentPsychContext(String studentId) {
        try {
            List<Map<String, Object>> profiles = psychSvcWebClient.get()
                    .uri("/api/v1/psych/profiles?studentId=" + studentId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .onErrorReturn(List.of())
                    .block(Duration.ofSeconds(5));

            if (profiles == null || profiles.isEmpty()) {
                return null;
            }

            Map<String, Object> p = profiles.get(0);
            double openness          = toDouble(p.get("openness"));
            double conscientiousness = toDouble(p.get("conscientiousness"));
            double extraversion      = toDouble(p.get("extraversion"));
            double agreeableness     = toDouble(p.get("agreeableness"));
            double neuroticism       = toDouble(p.get("neuroticism"));
            String riasec            = p.get("riasecCode") != null ? p.get("riasecCode").toString() : "Not yet generated";
            String status            = p.get("status") != null ? p.get("status").toString() : "UNKNOWN";

            if (openness == 0 && conscientiousness == 0 && extraversion == 0) {
                return "Student Psychometric Profile: Assessment not yet completed (profile status: " + status + ").";
            }

            String dominant = dominantLearningStyle(openness, conscientiousness, extraversion);
            return String.format(
                    "Student Psychometric Profile (Big Five Assessment — scores out of 100):\n" +
                    "- Openness: %.0f/100 (intellectual curiosity, creativity)\n" +
                    "- Conscientiousness: %.0f/100 (organisation, goal-directed behaviour)\n" +
                    "- Extraversion: %.0f/100 (social energy, confidence in groups)\n" +
                    "- Agreeableness: %.0f/100 (empathy, cooperative teamwork)\n" +
                    "- Neuroticism: %.0f/100 (stress sensitivity — lower is more resilient)\n" +
                    "RIASEC Career Code: %s\n" +
                    "Dominant Learning Style: %s\n" +
                    "Use this real data when answering questions about the child's personality, strengths, weaknesses, or career paths.",
                    openness * 100, conscientiousness * 100, extraversion * 100,
                    agreeableness * 100, neuroticism * 100, riasec, dominant);
        } catch (Exception e) {
            log.warn("Could not fetch psych context for student {}: {}", studentId, e.getMessage());
            return null;
        }
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (NumberFormatException e) { return 0.0; }
    }

    private String dominantLearningStyle(double o, double c, double e) {
        double visual      = o * 0.6 + c * 0.4;
        double auditory    = e * 0.5 + o * 0.5;
        double kinesthetic = e * 0.4 + c * 0.6;
        double reading     = c * 0.7 + o * 0.3;
        double max = Math.max(Math.max(visual, auditory), Math.max(kinesthetic, reading));
        if (max == visual)      return String.format("Visual (%.0f%%)", visual * 100);
        if (max == auditory)    return String.format("Auditory (%.0f%%)", auditory * 100);
        if (max == kinesthetic) return String.format("Kinesthetic (%.0f%%)", kinesthetic * 100);
        return String.format("Reading/Writing (%.0f%%)", reading * 100);
    }

    private void validateOwnership(CopilotConversation conversation, String parentId) {
        if (!conversation.getParentId().equals(parentId)) {
            throw new com.edutech.parent.application.exception.ParentAccessDeniedException();
        }
    }

    private String buildTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.isBlank()) {
            return "New Conversation";
        }
        return firstMessage.length() > 80 ? firstMessage.substring(0, 80) + "..." : firstMessage;
    }

    private ConversationResponse toResponse(CopilotConversation c) {
        List<MessageResponse> messageResponses = c.getMessages().stream()
                .map(m -> new MessageResponse(m.getId(), m.getRole(), m.getContent(), m.getSentAt()))
                .toList();
        return new ConversationResponse(
                c.getId(),
                c.getParentId(),
                c.getStudentId(),
                c.getTitle(),
                c.getStatus().name(),
                messageResponses,
                c.getCreatedAt()
        );
    }
}
