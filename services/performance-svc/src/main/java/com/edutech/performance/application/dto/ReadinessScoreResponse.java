package com.edutech.performance.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReadinessScoreResponse(
        UUID id,
        UUID studentId,
        UUID enrollmentId,
        BigDecimal ersScore,
        BigDecimal syllabusCoveragePercent,
        BigDecimal mockTestTrendScore,
        BigDecimal masteryAverage,
        BigDecimal accuracyConsistency,
        Integer projectedRank,
        BigDecimal projectedPercentile,
        Instant computedAt
) {
}
