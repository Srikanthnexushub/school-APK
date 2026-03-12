package com.edutech.notification.application.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationHistoryResponse(
        UUID id,
        String channel,
        String subject,
        String body,
        String status,
        int retryCount,
        Instant createdAt,
        Instant sentAt,
        String notificationType,
        String actionUrl,
        Instant readAt
) {}
