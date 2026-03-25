// src/main/java/com/edutech/center/domain/model/Announcement.java
package com.edutech.center.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Targeted announcement from CENTER_ADMIN or TEACHER to a specific audience.
 * Distinct from banners (platform-level marketing).
 * Delivered via notification-svc through Kafka NotificationSendEvent.
 */
@Entity
@Table(name = "announcements", schema = "center_schema")
public class Announcement {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "center_id", nullable = false, updatable = false)
    private UUID centerId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "sent_by_id", nullable = false, updatable = false)
    private UUID sentById;

    @Column(name = "sent_by_role", nullable = false, length = 50, updatable = false)
    private String sentByRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30, updatable = false)
    private AnnouncementTargetType targetType;

    @Column(name = "target_id", updatable = false)
    private UUID targetId;

    @Column(name = "target_role", length = 30, updatable = false)
    private String targetRole;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Announcement() {}

    private Announcement(UUID id, UUID centerId, String title, String body,
                         UUID sentById, String sentByRole,
                         AnnouncementTargetType targetType, UUID targetId, String targetRole,
                         Instant scheduledAt, Instant expiresAt) {
        this.id = id;
        this.centerId = centerId;
        this.title = title;
        this.body = body;
        this.sentById = sentById;
        this.sentByRole = sentByRole;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetRole = targetRole;
        this.scheduledAt = scheduledAt;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public static Announcement create(UUID centerId, String title, String body,
                                      UUID sentById, String sentByRole,
                                      AnnouncementTargetType targetType, UUID targetId, String targetRole,
                                      Instant scheduledAt, Instant expiresAt) {
        return new Announcement(UUID.randomUUID(), centerId, title, body, sentById, sentByRole,
                targetType, targetId, targetRole, scheduledAt, expiresAt);
    }

    public void markSent() { this.sentAt = Instant.now(); }
    public void expire()   { this.expiresAt = Instant.now(); }

    public boolean isDue() {
        return sentAt == null && (scheduledAt == null || !scheduledAt.isAfter(Instant.now()));
    }

    public UUID getId()             { return id; }
    public UUID getCenterId()       { return centerId; }
    public String getTitle()        { return title; }
    public String getBody()         { return body; }
    public UUID getSentById()       { return sentById; }
    public String getSentByRole()   { return sentByRole; }
    public AnnouncementTargetType getTargetType() { return targetType; }
    public UUID getTargetId()       { return targetId; }
    public String getTargetRole()   { return targetRole; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getSentAt()      { return sentAt; }
    public Instant getExpiresAt()   { return expiresAt; }
    public Instant getCreatedAt()   { return createdAt; }
}
