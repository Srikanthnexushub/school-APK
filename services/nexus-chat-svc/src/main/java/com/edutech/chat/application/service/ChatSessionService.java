package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.*;
import com.edutech.chat.domain.port.out.*;
import com.edutech.chat.infrastructure.adapter.in.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final ProactiveNudgeRepository nudgeRepo;
    private final ContextAggregatorService contextAggregator;
    private final SystemPromptBuilder promptBuilder;
    private final AiGatewayStreamPort aiGateway;
    private final ChatEventPublisherPort eventPublisher;
    @Qualifier("streamExecutor")
    private final ExecutorService streamExecutor;

    @Value("${chat.max-history-messages:20}")
    private int maxHistoryMessages;

    // ─────────────────────────────────────────────
    // START SESSION
    // ─────────────────────────────────────────────
    @Transactional
    public ChatSessionStartResult startSession(UUID userId, String userRole,
                                               UUID centerId, String pageContext,
                                               String jwtToken) {
        ChatSession session = ChatSession.create(userId, userRole, pageContext);
        session = sessionRepo.save(session);

        RoleContext ctx = contextAggregator.aggregate(userId, userRole, centerId, pageContext, jwtToken);
        String greeting = buildGreeting(ctx, pageContext);

        ChatMessage systemMsg = new ChatMessage();
        systemMsg.setSession(session);
        systemMsg.setRole(MessageRole.SYSTEM);
        systemMsg.setContent(promptBuilder.build(ctx));
        systemMsg.setMessageType(MessageType.TEXT);
        messageRepo.save(systemMsg);

        ChatMessage greetingMsg = ChatMessage.assistantMessage(session, greeting, 0, 0, 0);
        messageRepo.save(greetingMsg);
        session.recordMessage();
        session.recordMessage();
        session.setTitleFromFirstMessage(greeting);
        sessionRepo.save(session);

        eventPublisher.publishSessionStarted(session.getId(), userId);
        return new ChatSessionStartResult(session.getId(), greeting, ctx);
    }

    // ─────────────────────────────────────────────
    // STREAM MESSAGE
    // ─────────────────────────────────────────────
    public ResponseBodyEmitter streamMessage(UUID sessionId, UUID userId, UUID centerId,
                                              String userMessage, String pageContext,
                                              String jwtToken) {
        // 60s — enough for 70B-class model TTFT (~5–25s) plus full response generation (Fix #288)
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(60_000L);

        streamExecutor.execute(() -> {
            try {
                ChatSession session = sessionRepo.findByIdAndUserId(sessionId, userId)
                    .orElseThrow(() -> new SessionNotFoundException(sessionId));

                RoleContext ctx = contextAggregator.aggregate(
                    userId, session.getUserRole(), centerId, pageContext, jwtToken);

                List<Map<String, String>> history = buildHistory(sessionId);

                StringBuilder fullResponse = new StringBuilder();
                long startMs = System.currentTimeMillis();
                int[] tokenCounts = {0, 0};

                aiGateway.streamCompletion(
                    promptBuilder.build(ctx),
                    history,
                    userMessage,
                    new StreamTokenConsumer() {
                        @Override
                        public void onToken(String token) {
                            fullResponse.append(token);
                            tokenCounts[1]++;
                            try {
                                emitter.send("data: " + buildDeltaJson(token) + "\n\n");
                            } catch (IOException e) {
                                log.debug("Client disconnected during stream for session {}", sessionId);
                            }
                        }

                        @Override
                        public void onComplete(int inTok, int outTok, int latency) {
                            tokenCounts[0] = inTok > 0 ? inTok : tokenCounts[0];
                            tokenCounts[1] = outTok > 0 ? outTok : tokenCounts[1];

                            int totalLatency = (int)(System.currentTimeMillis() - startMs);
                            String cleanContent = removeActionJson(fullResponse.toString());

                            persistMessages(session, userMessage, cleanContent,
                                tokenCounts[0], tokenCounts[1], totalLatency);

                            try {
                                emitter.send("data: [DONE]\n\n");
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }

                            eventPublisher.publishMessageSent(
                                sessionId, userId, tokenCounts[0] + tokenCounts[1], totalLatency);
                        }

                        @Override
                        public void onError(Throwable t) {
                            log.error("Stream error for session {}: {}", sessionId, t.getMessage());
                            try {
                                emitter.send("data: " + buildDeltaJson(
                                    "Sorry, I ran into an issue. Please try again.") + "\n\n");
                                emitter.send("data: [DONE]\n\n");
                                emitter.complete();
                            } catch (IOException ex) {
                                emitter.completeWithError(ex);
                            }
                        }
                    }
                );

            } catch (SessionNotFoundException e) {
                try {
                    emitter.send("data: {\"error\":\"SESSION_NOT_FOUND\"}\n\n");
                    emitter.send("data: [DONE]\n\n");
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            } catch (Exception e) {
                log.error("Unexpected error in stream for session {}: {}", sessionId, e.getMessage());
                try {
                    emitter.send("data: [DONE]\n\n");
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

    // ─────────────────────────────────────────────
    // SEND MESSAGE (blocking fallback)
    // ─────────────────────────────────────────────
    @Transactional
    public SendMessageResponse sendMessage(UUID sessionId, UUID userId,
                                           String content, String pageContext, String jwtToken) {
        ChatSession session = sessionRepo.findByIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));

        ChatMessage userMsg = ChatMessage.userMessage(session, content);
        messageRepo.save(userMsg);
        session.recordMessage();
        sessionRepo.save(session);

        return new SendMessageResponse(userMsg.getId(), "Message recorded.", null);
    }

    // ─────────────────────────────────────────────
    // GET SESSIONS
    // ─────────────────────────────────────────────
    public List<ChatSessionSummaryDto> getSessionSummaries(UUID userId) {
        return sessionRepo.findActiveByUserId(userId).stream()
            .map(s -> new ChatSessionSummaryDto(
                s.getId(),
                s.getTitle(),
                null,
                s.getMessageCount(),
                s.getLastActiveAt()
            ))
            .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // GET MESSAGES
    // ─────────────────────────────────────────────
    public List<ChatMessageDto> getMessages(UUID sessionId, UUID userId) {
        sessionRepo.findByIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));

        return messageRepo.findAllBySessionId(sessionId).stream()
            .filter(m -> m.getRole() != MessageRole.SYSTEM)
            .map(m -> new ChatMessageDto(
                m.getId(),
                m.getRole().name(),
                m.getContent(),
                m.getMessageType().name(),
                m.getActionPayload(),
                m.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // DELETE SESSION
    // ─────────────────────────────────────────────
    @Transactional
    public void deleteSession(UUID sessionId, UUID userId) {
        ChatSession session = sessionRepo.findByIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
        session.archive();
        sessionRepo.save(session);
    }

    // ─────────────────────────────────────────────
    // NUDGES
    // ─────────────────────────────────────────────
    public List<ProactiveNudgeDto> getPendingNudges(UUID userId) {
        return nudgeRepo.findUndeliveredByUserId(userId, 10).stream()
            .map(n -> new ProactiveNudgeDto(
                n.getId(),
                n.getMessage(),
                n.getActionUrl(),
                n.getTriggerType().name(),
                n.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }

    @Transactional
    public void markNudgeOpened(UUID nudgeId, UUID userId) {
        nudgeRepo.findById(nudgeId).ifPresent(nudge -> {
            nudge.markOpened();
            nudgeRepo.save(nudge);
        });
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────
    private String buildGreeting(RoleContext ctx, String pageContext) {
        String name = ctx.fullName() != null && !ctx.fullName().isBlank()
            ? ctx.fullName().split(" ")[0] : "there";

        if (ctx instanceof StudentContext s) return buildStudentGreeting(s, name, pageContext);
        if (ctx instanceof TeacherContext)  return "Hi " + name + "! I\u2019m NexusChat, your AI teaching assistant. "
            + "Ask me about your batches, student progress, or exam creation.";
        if (ctx instanceof AdminContext)    return "Hi " + name + "! I\u2019m NexusChat. "
            + "I can help with center operations, batch health, teacher approvals, and more.";
        if (ctx instanceof ParentContext)   return "Hi " + name + "! I\u2019m NexusChat. "
            + "I can help you track your child\u2019s progress, fees, and attendance.";
        return "Hi " + name + "! I\u2019m NexusChat. How can I help you today?";
    }

    private String buildStudentGreeting(StudentContext s, String name, String pageContext) {
        Map<String, String> greetings = Map.of(
            "dashboard",    "Hi " + name + "! Your ERS is " + s.ersScore() + "/100. What would you like to work on today?",
            "performance",  "Hi " + name + "! I can see your performance data. Want me to explain your weak areas or create a study plan?",
            "assessments",  "Hi " + name + "! Ready for exam prep? I can explain past results or help you prepare for upcoming exams.",
            "fees",         "Hi " + name + "! I can help you understand your fee status. What would you like to know?",
            "attendance",   "Hi " + name + "! I can see your attendance summary. Any concerns I can help with?"
        );
        return greetings.getOrDefault(pageContext,
            "Hi " + name + "! I\u2019m NexusChat, your AI study partner. How can I help you today?");
    }

    private List<Map<String, String>> buildHistory(UUID sessionId) {
        return messageRepo.findLastNBySessionId(sessionId, maxHistoryMessages)
            .stream()
            .filter(m -> m.getRole() != MessageRole.SYSTEM)
            .map(m -> Map.of(
                "role", m.getRole().name().toLowerCase(),
                "content", m.getContent()
            ))
            .collect(Collectors.toList());
    }

    @Transactional
    private void persistMessages(ChatSession session, String userMessage,
                                  String assistantContent, int inTok, int outTok, int latency) {
        session.setTitleFromFirstMessage(userMessage);
        messageRepo.save(ChatMessage.userMessage(session, userMessage));
        messageRepo.save(ChatMessage.assistantMessage(session, assistantContent, inTok, outTok, latency));
        session.recordMessage();
        session.recordMessage();
        sessionRepo.save(session);
    }

    private String buildDeltaJson(String token) {
        String escaped = token.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r");
        return "{\"choices\":[{\"delta\":{\"content\":\"" + escaped + "\"}}]}";
    }

    private String removeActionJson(String content) {
        int start = content.indexOf("{\"action\":");
        if (start == -1) return content;
        int depth = 0, end = -1;
        for (int i = start; i < content.length(); i++) {
            if (content.charAt(i) == '{') depth++;
            else if (content.charAt(i) == '}') {
                depth--;
                if (depth == 0) { end = i; break; }
            }
        }
        return end == -1 ? content : (content.substring(0, start) + content.substring(end + 1)).trim();
    }
}
