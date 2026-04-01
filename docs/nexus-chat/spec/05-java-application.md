# NexusChat — Application Service Layer (Full Code)

## ChatSessionService.java

```java
package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.*;
import com.edutech.chat.domain.port.in.*;
import com.edutech.chat.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatSessionService implements
    StartSessionUseCase, SendMessageUseCase, StreamMessageUseCase,
    GetSessionHistoryUseCase, DeleteSessionUseCase {

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final ContextAggregatorService contextAggregator;
    private final SystemPromptBuilder promptBuilder;
    private final AiGatewayStreamPort aiGateway;
    private final ChatEventPublisherPort eventPublisher;
    private final ExecutorService streamExecutor;   // StreamThreadPoolConfig bean

    @Value("${chat.max-history-messages:20}")
    private int maxHistoryMessages;

    // ─────────────────────────────────────────────
    // START SESSION
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public ChatSessionStartResult startSession(UUID userId, String userRole,
                                               String pageContext, String jwtToken) {
        ChatSession session = ChatSession.create(userId, userRole, pageContext);
        session = sessionRepo.save(session);

        // Aggregate context (parallel, non-blocking)
        StudentContext ctx = contextAggregator.aggregate(userId, userRole, pageContext, jwtToken);

        // Build greeting message from context
        String greeting = buildGreeting(ctx, pageContext);

        // Persist system + greeting messages
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
        sessionRepo.save(session);

        eventPublisher.publishSessionStarted(session.getId(), userId);
        return new ChatSessionStartResult(session.getId(), greeting, ctx);
    }

    // ─────────────────────────────────────────────
    // STREAM MESSAGE (token-by-token via ResponseBodyEmitter)
    // ─────────────────────────────────────────────
    @Override
    public ResponseBodyEmitter streamMessage(UUID sessionId, UUID userId,
                                              String userMessage, String pageContext,
                                              String jwtToken) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(30_000L);

        streamExecutor.execute(() -> {
            try {
                ChatSession session = sessionRepo.findByIdAndUserId(sessionId, userId)
                    .orElseThrow(() -> new SessionNotFoundException(sessionId));

                // Refresh context if stale (>15min old)
                StudentContext ctx = contextAggregator.getOrRefresh(sessionId, userId, userRole(session),
                    pageContext, jwtToken);

                // Build messages array for LLM (system + last N messages + new user message)
                List<Map<String, String>> history = buildHistory(sessionId);

                StringBuilder fullResponse = new StringBuilder();
                long startMs = System.currentTimeMillis();
                int[] tokenCounts = {0, 0}; // [0]=input, [1]=output

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
                                emitter.send("{\"token\":" + escapeJson(token) +
                                    ",\"done\":false}\n");
                            } catch (IOException e) {
                                log.warn("Emitter write failed — client disconnected");
                            }
                        }

                        @Override
                        public void onComplete(int inTok, int outTok, int latency) {
                            tokenCounts[0] = inTok;
                            tokenCounts[1] = outTok;

                            // Extract action JSON if AI returned one
                            String actionJson = extractActionJson(fullResponse.toString());
                            String cleanContent = removeActionJson(fullResponse.toString());

                            // Persist both messages
                            persistMessages(session, userMessage, cleanContent,
                                tokenCounts[0], tokenCounts[1],
                                (int)(System.currentTimeMillis() - startMs));

                            try {
                                String donePayload = "{\"token\":\"\",\"done\":true" +
                                    (actionJson != null ? ",\"actionJson\":" + actionJson : "") +
                                    "}";
                                emitter.send(donePayload + "\n");
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            log.error("Stream error for session {}: {}", sessionId, t.getMessage());
                            try {
                                emitter.send("{\"token\":\"I'm having trouble connecting. " +
                                    "Please try again.\",\"done\":true}\n");
                                emitter.complete();
                            } catch (IOException ex) {
                                emitter.completeWithError(ex);
                            }
                        }
                    }
                );

            } catch (SessionNotFoundException e) {
                try {
                    emitter.send("{\"error\":\"Session not found\",\"done\":true}\n");
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────
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

    private String buildGreeting(StudentContext ctx, String pageContext) {
        String name = ctx.fullName().split(" ")[0]; // first name only
        Map<String, String> greetings = Map.of(
            "dashboard",    "Hi " + name + "! Your ERS is " + ctx.ersScore() + "/100. What would you like to work on today?",
            "performance",  "Hi " + name + "! I can see your performance data. Want me to explain your weak areas or create a study plan?",
            "assessments",  "Hi " + name + "! Ready for exam prep? I can explain past results or help you prepare for upcoming exams.",
            "fees",         "Hi " + name + "! I can help you understand your fee status. What would you like to know?",
            "attendance",   "Hi " + name + "! I can see your attendance summary. Any concerns I can help with?"
        );
        return greetings.getOrDefault(pageContext,
            "Hi " + name + "! I'm your NexusChat assistant. How can I help you today?");
    }

    private String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private String extractActionJson(String content) {
        int start = content.indexOf("{\"action\":");
        if (start == -1) return null;
        int depth = 0, end = -1;
        for (int i = start; i < content.length(); i++) {
            if (content.charAt(i) == '{') depth++;
            else if (content.charAt(i) == '}') { depth--; if (depth == 0) { end = i; break; } }
        }
        return end == -1 ? null : content.substring(start, end + 1);
    }

    private String removeActionJson(String content) {
        int start = content.indexOf("{\"action\":");
        if (start == -1) return content;
        int depth = 0, end = -1;
        for (int i = start; i < content.length(); i++) {
            if (content.charAt(i) == '{') depth++;
            else if (content.charAt(i) == '}') { depth--; if (depth == 0) { end = i; break; } }
        }
        return end == -1 ? content : (content.substring(0, start) + content.substring(end + 1)).trim();
    }

    private String userRole(ChatSession session) { return session.getUserRole(); }
}
```

## ContextAggregatorService.java

```java
package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.*;
import com.edutech.chat.infrastructure.adapter.out.webclient.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple5;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContextAggregatorService {

    private final StudentProfileWebClientAdapter profileClient;
    private final PerformanceWebClientAdapter performanceClient;
    private final AiMentorWebClientAdapter mentorClient;
    private final AssessWebClientAdapter assessClient;
    private final CenterWebClientAdapter centerClient;

    @Value("${context.total-timeout-ms:800}")
    private long totalTimeoutMs;

    public StudentContext aggregate(UUID userId, String userRole, String pageContext, String jwt) {
        try {
            Tuple5<StudentProfileDto, PerformanceDto, MentorContextDto, AssessContextDto, CenterContextDto> tuple =
                Mono.zip(
                    profileClient.fetchProfile(userId, jwt)
                        .onErrorReturn(StudentProfileDto.empty(userId)),
                    performanceClient.fetchPerformance(userId, jwt)
                        .onErrorReturn(PerformanceDto.empty()),
                    mentorClient.fetchMentorContext(userId, jwt)
                        .onErrorReturn(MentorContextDto.empty()),
                    assessClient.fetchAssessContext(userId, jwt)
                        .onErrorReturn(AssessContextDto.empty()),
                    centerClient.fetchCenterContext(userId, jwt)
                        .onErrorReturn(CenterContextDto.empty())
                )
                .block(Duration.ofMillis(totalTimeoutMs));

            if (tuple == null) {
                log.warn("Context aggregation timed out for userId={}", userId);
                return StudentContext.empty(userId, pageContext);
            }

            return mapToStudentContext(tuple, userId, pageContext);

        } catch (Exception e) {
            log.error("Context aggregation failed for userId={}: {}", userId, e.getMessage());
            return StudentContext.empty(userId, pageContext);
        }
    }

    private StudentContext mapToStudentContext(
        Tuple5<StudentProfileDto, PerformanceDto, MentorContextDto, AssessContextDto, CenterContextDto> t,
        UUID userId, String pageContext) {

        StudentProfileDto profile = t.getT1();
        PerformanceDto perf = t.getT2();
        MentorContextDto mentor = t.getT3();
        AssessContextDto assess = t.getT4();
        CenterContextDto center = t.getT5();

        List<WeakAreaSummary> weakAreas = perf.weakAreas().stream()
            .limit(3)
            .map(w -> new WeakAreaSummary(w.subject(), w.topicName(), w.masteryPercent(), w.severity()))
            .toList();

        List<MasterySummary> mastery = perf.subjectMastery().stream()
            .map(m -> new MasterySummary(m.subject(), m.masteryPercent(), m.masteryLevel()))
            .toList();

        return new StudentContext(
            userId,
            profile.fullName(),
            profile.currentClass(),
            profile.board(),
            profile.stream(),
            profile.subjects() != null ? profile.subjects() : List.of(),
            profile.targetYear(),
            perf.ersScore() != null ? BigDecimal.valueOf(perf.ersScore()) : BigDecimal.ZERO,
            resolveRisk(perf.ersScore()),
            weakAreas,
            mastery,
            mentor.activeStudyPlan() != null
                ? Optional.of(new StudyPlanSummary(
                    mentor.activeStudyPlan().id(),
                    mentor.activeStudyPlan().title(),
                    mentor.activeStudyPlan().totalItems(),
                    mentor.activeStudyPlan().completedItems(),
                    mentor.activeStudyPlan().targetExamDate()))
                : Optional.empty(),
            mentor.pendingDoubtCount(),
            assess.lastExam() != null
                ? Optional.of(new RecentExamSummary(
                    assess.lastExam().examId(),
                    assess.lastExam().title(),
                    assess.lastExam().scoredMarks(),
                    assess.lastExam().totalMarks(),
                    assess.lastExam().percentage(),
                    assess.lastExam().letterGrade(),
                    assess.lastExam().submittedAt()))
                : Optional.empty(),
            assess.examsThisMonth(),
            Optional.ofNullable(center.batchName()),
            Optional.ofNullable(center.centerName()),
            pageContext
        );
    }

    private String resolveRisk(Double ers) {
        if (ers == null) return "UNKNOWN";
        if (ers < 40) return "AT_RISK";
        if (ers < 70) return "NEEDS_ATTENTION";
        if (ers < 90) return "ON_TRACK";
        return "EXCELLENT";
    }
}
```

## SystemPromptBuilder.java

```java
package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Component
public class SystemPromptBuilder {

    public String build(StudentContext ctx) {
        return """
            You are NexusChat, an intelligent AI academic assistant built into NexusEd platform.
            You have access to this student's real-time academic data below. Use it proactively.

            ## STUDENT PROFILE
            Name: %s | Class: %s | Board: %s | Stream: %s | Target Year: %d
            Subjects: %s

            ## CURRENT PERFORMANCE
            Exam Readiness Score (ERS): %.1f/100 — %s
            Subject Mastery:
            %s

            ## WEAK AREAS (requires attention)
            %s

            ## RECENT ACTIVITY
            Last Exam: %s
            Active Study Plan: %s
            Pending Doubts: %d
            Exams This Month: %d

            ## ENROLLMENT
            %s

            ## PAGE CONTEXT
            Student is currently on the "%s" page. Tailor your response to this context.

            ## CAPABILITIES
            You can answer academic doubts, explain concepts, help with exam prep,
            and analyse this student's performance data above.

            You can EXECUTE ACTIONS by including a JSON block at the end of your response:
            {"action": "CREATE_STUDY_PLAN", "params": {"subject": "Physics", "days": 7}}
            {"action": "SCHEDULE_REMINDER", "params": {"message": "Review Newton's Laws", "dueInHours": 24}}
            {"action": "NAVIGATE", "params": {"path": "/performance", "label": "Performance page"}}
            {"action": "SHOW_WEAK_AREAS", "params": {}}
            Only include the action JSON when it truly helps the student. Never fabricate data.

            ## RESPONSE STYLE
            - Address student by first name (%s)
            - Be warm, encouraging, never judgmental about low scores
            - Keep responses focused (2-3 paragraphs max unless asked for detail)
            - For weak areas, always suggest specific, actionable study techniques
            - If student sounds stressed or frustrated, acknowledge it first
            - Today's date: %s
            """.formatted(
                ctx.fullName(), ctx.currentClass(), ctx.board(),
                ctx.stream() != null ? ctx.stream() : "Not set",
                ctx.targetYear(),
                ctx.subjects().isEmpty() ? "Not set" : String.join(", ", ctx.subjects()),
                ctx.ersScore().doubleValue(), ctx.ersRisk(),
                formatMastery(ctx),
                formatWeakAreas(ctx),
                formatLastExam(ctx),
                formatStudyPlan(ctx),
                ctx.pendingDoubtCount(),
                ctx.examsThisMonth(),
                formatEnrollment(ctx),
                ctx.currentPage(),
                ctx.fullName().split(" ")[0],
                LocalDate.now()
        );
    }

    private String formatMastery(StudentContext ctx) {
        if (ctx.subjectMastery().isEmpty()) return "  No mastery data yet";
        return ctx.subjectMastery().stream()
            .map(m -> "  - " + m.subject() + ": " + String.format("%.1f", m.masteryPercent()) +
                      "% (" + m.masteryLevel() + ")")
            .collect(Collectors.joining("\n"));
    }

    private String formatWeakAreas(StudentContext ctx) {
        if (ctx.weakAreas().isEmpty()) return "  No critical weak areas detected";
        return ctx.weakAreas().stream()
            .map(w -> "  - " + w.subject() + " → " + w.topic() +
                      " (" + String.format("%.1f", w.masteryPercent()) + "% mastery, " + w.severity() + ")")
            .collect(Collectors.joining("\n"));
    }

    private String formatLastExam(StudentContext ctx) {
        return ctx.lastExam()
            .map(e -> e.title() + " — " + String.format("%.0f", e.percentage()) +
                      "% (" + e.letterGrade() + "), submitted " + e.submittedAt().toString().substring(0, 10))
            .orElse("No exams taken yet");
    }

    private String formatStudyPlan(StudentContext ctx) {
        return ctx.activeStudyPlan()
            .map(p -> p.title() + " — " + p.completedItems() + "/" + p.totalItems() + " items complete")
            .orElse("No active study plan");
    }

    private String formatEnrollment(StudentContext ctx) {
        String batch = ctx.batchName().orElse("Not enrolled in a batch");
        String center = ctx.centerName().orElse("No center linked");
        return "Batch: " + batch + " | Center: " + center;
    }
}
```

## ProactiveNudgeService.java

```java
package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.*;
import com.edutech.chat.domain.port.out.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProactiveNudgeService {

    private final ProactiveNudgeRepository nudgeRepo;
    private final ChatEventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.notification-send}")
    private String notificationTopic;

    @Transactional
    public void handleExamSubmitted(UUID studentId, String examTitle, double percentage,
                                     int wrongCount, UUID examId) {
        String message = buildExamNudge(examTitle, percentage, wrongCount);
        String actionUrl = "/chat?nudge_exam=" + examId;

        ProactiveNudge nudge = new ProactiveNudge();
        nudge.setUserId(studentId);
        nudge.setTriggerType(NudgeTriggerType.EXAM_SUBMITTED);
        nudge.setTriggerPayload(Map.of(
            "examId", examId.toString(),
            "examTitle", examTitle,
            "percentage", percentage,
            "wrongCount", wrongCount
        ));
        nudge.setMessage(message);
        nudge.setActionUrl(actionUrl);
        nudgeRepo.save(nudge);

        eventPublisher.publishNotification(studentId, "NexusChat has insights for you",
            message, "NEXUS_CHAT_NUDGE", actionUrl);
        nudge.markDelivered();
        nudgeRepo.save(nudge);
        log.info("Proactive nudge delivered to student {} for exam {}", studentId, examTitle);
    }

    @Transactional
    public void handleWeakAreaCritical(UUID studentId, String subject, String topic) {
        String message = "⚠️ " + subject + " — " + topic + " has dropped to a critical weak area. " +
            "Let me build you a focused recovery plan. Open NexusChat to get started.";

        ProactiveNudge nudge = new ProactiveNudge();
        nudge.setUserId(studentId);
        nudge.setTriggerType(NudgeTriggerType.WEAK_AREA_CRITICAL);
        nudge.setTriggerPayload(Map.of("subject", subject, "topic", topic));
        nudge.setMessage(message);
        nudge.setActionUrl("/chat?page=performance");
        nudgeRepo.save(nudge);

        eventPublisher.publishNotification(studentId, "Action needed on " + subject,
            message, "NEXUS_CHAT_NUDGE", "/chat?page=performance");
        nudge.markDelivered();
        nudgeRepo.save(nudge);
    }

    private String buildExamNudge(String examTitle, double percentage, int wrongCount) {
        String firstName = "";  // will be filled from context — empty here is fine
        if (percentage >= 85) {
            return "Great work on " + examTitle + "! You scored " +
                String.format("%.0f", percentage) + "%. " +
                "Want me to reinforce the " + wrongCount + " questions you got wrong to reach 100%?";
        } else if (percentage >= 60) {
            return "You completed " + examTitle + " with " +
                String.format("%.0f", percentage) + "%. There are " + wrongCount +
                " questions worth reviewing. Want a quick debrief?";
        } else {
            return "You completed " + examTitle + ". The score was " +
                String.format("%.0f", percentage) + "% — let's work through the " +
                wrongCount + " questions you missed together. Open NexusChat for a detailed debrief.";
        }
    }
}
```
