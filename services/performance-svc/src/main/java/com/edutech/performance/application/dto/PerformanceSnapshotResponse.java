package com.edutech.performance.application.dto;

import com.edutech.performance.domain.model.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PerformanceSnapshotResponse(
        UUID id,
        BigDecimal ersScore,
        BigDecimal theta,
        BigDecimal percentile,
        RiskLevel riskLevel,
        BigDecimal dropoutRiskScore,
        Instant snapshotAt
) {
}
