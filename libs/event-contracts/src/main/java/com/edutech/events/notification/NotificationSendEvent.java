package com.edutech.events.notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable Kafka event published by any service that wants to trigger a notification.
 * Consumed exclusively by notification-svc.
 *
 * <p>Topic: {@code KAFKA_TOPIC_NOTIFICATION_SEND}</p>
 *
 * <p>channel values: EMAIL | PUSH | IN_APP | SMS</p>
 */
public record NotificationSendEvent(
        UUID eventId,
        UUID recipientId,
        String recipientEmail,
        String recipientPhone,
        String channel,
        String subject,
        String body,
        Map<String, String> metadata,
        Instant occurredAt
) {
    /** Convenience factory for EMAIL notifications. */
    public static NotificationSendEvent email(UUID recipientId, String recipientEmail,
                                               String subject, String body,
                                               Map<String, String> metadata) {
        return new NotificationSendEvent(
                UUID.randomUUID(),
                recipientId,
                recipientEmail,
                null,
                "EMAIL",
                subject,
                body,
                metadata != null ? metadata : Map.of(),
                Instant.now()
        );
    }

    /** Convenience factory for PUSH notifications. */
    public static NotificationSendEvent push(UUID recipientId, String subject, String body,
                                              Map<String, String> metadata) {
        return new NotificationSendEvent(
                UUID.randomUUID(),
                recipientId,
                null,
                null,
                "PUSH",
                subject,
                body,
                metadata != null ? metadata : Map.of(),
                Instant.now()
        );
    }

    /** Convenience factory for IN_APP notifications. */
    public static NotificationSendEvent inApp(UUID recipientId, String subject, String body,
                                               Map<String, String> metadata) {
        return new NotificationSendEvent(
                UUID.randomUUID(),
                recipientId,
                null,
                null,
                "IN_APP",
                subject,
                body,
                metadata != null ? metadata : Map.of(),
                Instant.now()
        );
    }

    /** Convenience factory for SMS notifications. */
    public static NotificationSendEvent sms(UUID recipientId, String recipientPhone,
                                             String subject, String body,
                                             Map<String, String> metadata) {
        return new NotificationSendEvent(
                UUID.randomUUID(),
                recipientId,
                null,
                recipientPhone,
                "SMS",
                subject,
                body,
                metadata != null ? metadata : Map.of(),
                Instant.now()
        );
    }
}
