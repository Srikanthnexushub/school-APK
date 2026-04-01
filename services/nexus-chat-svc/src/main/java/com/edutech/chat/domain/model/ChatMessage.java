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
