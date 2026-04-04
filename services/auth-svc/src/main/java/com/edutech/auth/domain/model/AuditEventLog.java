// src/main/java/com/edutech/auth/domain/model/AuditEventLog.java
package com.edutech.auth.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit event log entry.
 *
 * Each row corresponds to one event consumed from the audit-immutable Kafka topic.
 * This entity is append-only — no update or delete operations are ever performed.
 *
 * Design rules:
 * - No Spring annotations (hexagonal architecture — domain layer is framework-free).
 * - No public setters — all fields set via constructor only.
 * - @Column(updatable = false) on every field enforces immutability at JPA level.
 */
@Entity
@Table(name = "audit_events_log", schema = "auth_schema")
public class AuditEventLog {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_type", updatable = false, nullable = false, length = 100)
    private String eventType;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "action", updatable = false, nullable = false, length = 200)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", updatable = false, columnDefinition = "jsonb")
    private String details;

    @Column(name = "ip_address", updatable = false, length = 50)
    private String ipAddress;

    @Column(name = "timestamp", updatable = false, nullable = false)
    private Instant timestamp;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    // Required by JPA — not for application use
    protected AuditEventLog() {}

    private AuditEventLog(UUID id, String eventType, UUID userId, String action,
                          String details, String ipAddress, Instant timestamp) {
        this.id = id;
        this.eventType = eventType;
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
        this.createdAt = Instant.now();
    }

    /**
     * Factory method — only way to create a new AuditEventLog entry.
     */
    public static AuditEventLog create(String eventType, UUID userId, String action,
                                       String details, String ipAddress, Instant timestamp) {
        return new AuditEventLog(
                UUID.randomUUID(), eventType, userId, action, details, ipAddress, timestamp
        );
    }

    // Read-only accessors
    public UUID getId()          { return id; }
    public String getEventType() { return eventType; }
    public UUID getUserId()      { return userId; }
    public String getAction()    { return action; }
    public String getDetails()   { return details; }
    public String getIpAddress() { return ipAddress; }
    public Instant getTimestamp() { return timestamp; }
    public Instant getCreatedAt() { return createdAt; }
}
