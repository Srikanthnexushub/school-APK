package com.edutech.center.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * AI-generated attendance insight for a single student.
 * riskLevel: HIGH | MEDIUM | LOW | NONE
 * trend: IMPROVING | DECLINING | STABLE
 * engagementScore: 0-100 composite score computed from attendance%, streak, trend, consistency
 */
public record AiAttendanceInsight(
        UUID studentId,
        String studentName,
        String riskLevel,
        String insight,
        String suggestedAction,
        BigDecimal predictedEomPercent,
        int engagementScore,
        String trend
) {}
