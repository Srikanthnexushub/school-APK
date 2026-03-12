package com.edutech.notification.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", schema = "notification_schema")
public class Notification {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    /** Semantic type used by the frontend to render the notification differently. */
    @Column(name = "notification_type", length = 50)
    private String notificationType;

    /** Timestamp when the user dismissed/read this in-app notification. */
    @Column(name = "read_at")
    private Instant readAt;

    /** Deep-link to the page associated with this notification. */
    @Column(name = "action_url", length = 500)
    private String actionUrl;

    /** Phone number used for SMS channel delivery (E.164 or local format). */
    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Notification() {
    }

    private Notification(UUID id, UUID recipientId, String recipientEmail, String recipientPhone,
                         NotificationChannel channel, String subject, String body,
                         String notificationType, String actionUrl) {
        this.id = id;
        this.recipientId = recipientId;
        this.recipientEmail = recipientEmail;
        this.recipientPhone = recipientPhone;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.notificationType = notificationType;
        this.actionUrl = actionUrl;
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = Instant.now();
    }

    public static Notification create(UUID recipientId, String recipientEmail,
                                      NotificationChannel channel, String subject, String body) {
        return new Notification(UUID.randomUUID(), recipientId, recipientEmail, null,
                channel, subject, body, null, null);
    }

    public static Notification create(UUID recipientId, String recipientEmail,
                                      NotificationChannel channel, String subject, String body,
                                      String notificationType, String actionUrl) {
        return new Notification(UUID.randomUUID(), recipientId, recipientEmail, null,
                channel, subject, body, notificationType, actionUrl);
    }

    public static Notification create(UUID recipientId, String recipientEmail, String recipientPhone,
                                      NotificationChannel channel, String subject, String body,
                                      String notificationType, String actionUrl) {
        return new Notification(UUID.randomUUID(), recipientId, recipientEmail, recipientPhone,
                channel, subject, body, notificationType, actionUrl);
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    /** Mark this in-app notification as read by the recipient. */
    public void markRead() {
        if (this.readAt == null) {
            this.readAt = Instant.now();
        }
    }

    public boolean isRead() {
        return this.readAt != null;
    }

    public UUID getId() { return id; }
    public UUID getRecipientId() { return recipientId; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getRecipientPhone() { return recipientPhone; }
    public NotificationChannel getChannel() { return channel; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public NotificationStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
    public String getNotificationType() { return notificationType; }
    public Instant getReadAt() { return readAt; }
    public String getActionUrl() { return actionUrl; }
    public long getVersion() { return version; }
}
