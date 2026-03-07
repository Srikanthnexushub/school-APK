package com.edutech.performance.domain.event;

import com.edutech.performance.domain.model.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DropoutRiskEscalatedEvent(
        String eventId,
        UUID studentId,
        UUID enrollmentId,
        RiskLevel riskLevel,
        BigDecimal riskScore,
        String primaryFactor,
        Instant occurredAt
) {
}
