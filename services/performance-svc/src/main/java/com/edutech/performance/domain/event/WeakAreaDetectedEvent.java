package com.edutech.performance.domain.event;

import com.edutech.performance.domain.model.ErrorType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WeakAreaDetectedEvent(
        String eventId,
        UUID studentId,
        UUID enrollmentId,
        String subject,
        String topicName,
        BigDecimal masteryPercent,
        ErrorType errorType,
        Instant occurredAt
) {
}
