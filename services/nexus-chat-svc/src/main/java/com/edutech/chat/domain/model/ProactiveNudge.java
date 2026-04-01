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
