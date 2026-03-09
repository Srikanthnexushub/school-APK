package com.edutech.parent.application.service;

import com.edutech.parent.application.dto.ConversationResponse;
import com.edutech.parent.application.dto.MessageResponse;
import com.edutech.parent.application.exception.ConversationNotFoundException;
import com.edutech.parent.domain.model.CopilotConversation;
import com.edutech.parent.domain.port.out.CopilotConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
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
    private final WebClient aiGatewayWebClient;
    private final int timeoutSeconds;

    public CopilotService(
            CopilotConversationRepository conversationRepository,
            WebClient aiGatewayWebClient,
            @Value("${ai-gateway.timeout-seconds:30}") int timeoutSeconds) {
        this.conversationRepository = conversationRepository;
        this.aiGatewayWebClient = aiGatewayWebClient;
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

        String aiReply = callAiGateway(initialMessage, List.of());
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

        conversation.addMessage("user", userMessage);

        // Build history for context window (exclude the just-added user message from prior history)
        List<Map<String, String>> history = buildHistory(conversation);
        String aiReply = callAiGateway(userMessage, history);
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

    private String callAiGateway(String message, List<Map<String, String>> history) {
        String historyText = history.stream()
                .map(m -> m.get("role") + ": " + m.get("content"))
                .collect(Collectors.joining("\n"));
        String systemPrompt = "You are the NexusEd Parent Copilot. Help parents understand their child's academic progress, fees, attendance, weak areas, and exam schedules. Be concise and supportive.\n\nConversation history:\n" + historyText;
        Map<String, Object> requestBody = Map.of(
                "requesterId", "parent-copilot",
                "systemPrompt", systemPrompt,
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
