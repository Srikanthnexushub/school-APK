# NexusChat — Java Domain Layer (Full Code)

## ChatSession.java

```java
package com.edutech.chat.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(schema = "chat_schema", name = "chat_sessions")
@Getter @Setter @NoArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_role", nullable = false, length = 30)
    private String userRole;

    @Column(name = "page_context", length = 100)
    private String pageContext;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "message_count", nullable = false)
    private int messageCount = 0;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();

    // Domain factory
    public static ChatSession create(UUID userId, String userRole, String pageContext) {
        ChatSession s = new ChatSession();
        s.userId = userId;
        s.userRole = userRole;
        s.pageContext = pageContext;
        return s;
    }

    // Domain behaviour
    public void setTitleFromFirstMessage(String content) {
        if (this.title == null && content != null && !content.isBlank()) {
            this.title = content.substring(0, Math.min(60, content.length())).trim();
        }
    }

    public void recordMessage() {
        this.messageCount++;
        this.lastActiveAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.status = SessionStatus.ARCHIVED;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return SessionStatus.ACTIVE.equals(this.status) && this.deletedAt == null;
    }
}
```

## ChatMessage.java

```java
package com.edutech.chat.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(schema = "chat_schema", name = "chat_messages")
@Getter @Setter @NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MessageRole role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "message_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private MessageType messageType = MessageType.TEXT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_payload")
    private Map<String, Object> actionPayload;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static ChatMessage userMessage(ChatSession session, String content) {
        ChatMessage m = new ChatMessage();
        m.session = session;
        m.role = MessageRole.USER;
        m.content = content;
        m.messageType = MessageType.TEXT;
        return m;
    }

    public static ChatMessage assistantMessage(ChatSession session, String content,
                                                int inputTokens, int outputTokens, int latencyMs) {
        ChatMessage m = new ChatMessage();
        m.session = session;
        m.role = MessageRole.ASSISTANT;
        m.content = content;
        m.messageType = MessageType.TEXT;
        m.inputTokens = inputTokens;
        m.outputTokens = outputTokens;
        m.latencyMs = latencyMs;
        return m;
    }

    public static ChatMessage actionResult(ChatSession session, String content,
                                            Map<String, Object> payload) {
        ChatMessage m = new ChatMessage();
        m.session = session;
        m.role = MessageRole.ASSISTANT;
        m.content = content;
        m.messageType = MessageType.ACTION_RESULT;
        m.actionPayload = payload;
        return m;
    }
}
```

## Enums

```java
// MessageRole.java
package com.edutech.chat.domain.model;
public enum MessageRole { USER, ASSISTANT, SYSTEM }

// MessageType.java
package com.edutech.chat.domain.model;
public enum MessageType { TEXT, ACTION_RESULT, CONTEXT_CARD }

// SessionStatus.java
package com.edutech.chat.domain.model;
public enum SessionStatus { ACTIVE, ARCHIVED }

// NudgeTriggerType.java
package com.edutech.chat.domain.model;
public enum NudgeTriggerType {
    EXAM_SUBMITTED, WEAK_AREA_CRITICAL, STUDY_PLAN_CREATED, INACTIVITY, EXAM_APPROACHING
}
```

## ProactiveNudge.java

```java
package com.edutech.chat.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(schema = "chat_schema", name = "proactive_nudges")
@Getter @Setter @NoArgsConstructor
public class ProactiveNudge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "trigger_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NudgeTriggerType triggerType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_payload", nullable = false)
    private Map<String, Object> triggerPayload;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "action_url", length = 255)
    private String actionUrl;

    @Column(nullable = false)
    private boolean delivered = false;

    @Column(nullable = false)
    private boolean opened = false;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public void markDelivered() {
        this.delivered = true;
        this.deliveredAt = Instant.now();
    }

    public void markOpened() {
        this.opened = true;
        this.openedAt = Instant.now();
    }
}
```

## StudentContext.java (Value Object — assembled by ContextEngine)

```java
package com.edutech.chat.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record StudentContext(
    UUID userId,
    String fullName,
    String currentClass,
    String board,
    String stream,
    List<String> subjects,
    int targetYear,
    BigDecimal ersScore,
    String ersRisk,
    List<WeakAreaSummary> weakAreas,
    List<MasterySummary> subjectMastery,
    Optional<StudyPlanSummary> activeStudyPlan,
    long pendingDoubtCount,
    Optional<RecentExamSummary> lastExam,
    int examsThisMonth,
    Optional<String> batchName,
    Optional<String> centerName,
    String currentPage
) {
    public static StudentContext empty(UUID userId, String currentPage) {
        return new StudentContext(userId, "Student", "Unknown", "CBSE", null,
            List.of(), 2026, BigDecimal.ZERO, "UNKNOWN", List.of(), List.of(),
            Optional.empty(), 0, Optional.empty(), 0,
            Optional.empty(), Optional.empty(), currentPage);
    }
}

// WeakAreaSummary.java
record WeakAreaSummary(String subject, String topic, double masteryPercent, String severity) {}

// MasterySummary.java
record MasterySummary(String subject, double masteryPercent, String masteryLevel) {}

// StudyPlanSummary.java
record StudyPlanSummary(UUID planId, String title, int totalItems, int completedItems, String targetDate) {}

// RecentExamSummary.java
record RecentExamSummary(UUID examId, String title, double scoredMarks, double totalMarks,
                          double percentage, String letterGrade, java.time.Instant submittedAt) {}
```

## ActionCommand.java (parsed from AI JSON blocks)

```java
package com.edutech.chat.domain.model;

import java.util.Map;

public record ActionCommand(
    String action,                    // e.g. "CREATE_STUDY_PLAN", "NAVIGATE", "SHOW_WEAK_AREAS"
    Map<String, Object> params
) {
    // Supported actions
    public static final String CREATE_STUDY_PLAN   = "CREATE_STUDY_PLAN";
    public static final String SCHEDULE_REMINDER   = "SCHEDULE_REMINDER";
    public static final String SHOW_WEAK_AREAS     = "SHOW_WEAK_AREAS";
    public static final String SHOW_FEES           = "SHOW_FEES";
    public static final String NAVIGATE            = "NAVIGATE";
    public static final String ENROLL_EXAM         = "ENROLL_EXAM";
}
```

## Port Interfaces

```java
// ChatSessionRepository.java
package com.edutech.chat.domain.port.out;

import com.edutech.chat.domain.model.ChatSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository {
    ChatSession save(ChatSession session);
    Optional<ChatSession> findByIdAndUserId(UUID sessionId, UUID userId);
    List<ChatSession> findActiveByUserId(UUID userId);
    Optional<ChatSession> findById(UUID sessionId);
}

// ChatMessageRepository.java
package com.edutech.chat.domain.port.out;

import com.edutech.chat.domain.model.ChatMessage;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage message);
    List<ChatMessage> findLastNBySessionId(UUID sessionId, int n);
    List<ChatMessage> findAllBySessionId(UUID sessionId);
}

// AiGatewayStreamPort.java
package com.edutech.chat.domain.port.out;

import java.util.List;
import java.util.Map;

public interface AiGatewayStreamPort {
    // Returns token stream — each item is a word/token from LLM
    // Falls back to blocking call if stream unavailable
    void streamCompletion(String systemPrompt, List<Map<String, String>> history,
                           String userMessage, StreamTokenConsumer consumer);
}

// StreamTokenConsumer.java (callback interface)
package com.edutech.chat.domain.port.out;

@FunctionalInterface
public interface StreamTokenConsumer {
    void onToken(String token);
    default void onComplete(int inputTokens, int outputTokens, int latencyMs) {}
    default void onError(Throwable t) {}
}
```
