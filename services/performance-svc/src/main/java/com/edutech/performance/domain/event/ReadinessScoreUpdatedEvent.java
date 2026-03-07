package com.edutech.performance.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReadinessScoreUpdatedEvent(
        String eventId,
        UUID studentId,
        UUID enrollmentId,
        BigDecimal ersScore,
        BigDecimal previousErsScore,
        Integer projectedRank,
        Instant occurredAt
) {
}
