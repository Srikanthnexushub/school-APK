package com.edutech.performance.application.dto;

import com.edutech.performance.domain.model.RiskLevel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PerformanceDashboardResponse(
        UUID studentId,
        UUID enrollmentId,
        ReadinessScoreResponse latestErs,
        List<WeakAreaResponse> topWeakAreas,
        List<SubjectMasteryResponse> subjectMastery,
        RiskLevel currentRiskLevel,
        BigDecimal dropoutRiskScore,
        PerformanceSnapshotResponse latestSnapshot
) {
}
