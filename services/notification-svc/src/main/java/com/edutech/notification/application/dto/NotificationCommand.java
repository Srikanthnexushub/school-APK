package com.edutech.notification.application.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Application-layer command used to trigger a notification send.
 * Built from a {@code NotificationSendEvent} consumed from Kafka.
 */
public record NotificationCommand(
        String channel,
        UUID recipientId,
        String recipientEmail,
        String recipientPhone,
        String subject,
        String body,
        Map<String, String> metadata
) {}
