package com.edutech.performance.domain.port.out;

import java.math.BigDecimal;
import java.util.List;

/**
 * Port for AI-enhanced dropout risk prediction.
 * Implementation calls the Python AI sidecar at /api/v1/predict-dropout-risk.
 * Falls back gracefully if sidecar is unavailable (circuit breaker).
 * The caller (application layer) must treat a {@code null} return as "sidecar unavailable"
 * and fall back to the rule-based {@code DropoutRiskCalculator}.
 */
public interface AiDropoutRiskPort {

    /**
     * Requests an AI-computed dropout risk prediction from the Python sidecar.
     *
     * @param studentId         UUID string of the student
     * @param ersScore          current Exam Readiness Score (0–100)
     * @param attendanceRate    attendance rate as a fraction 0.0–1.0
     * @param weeklyStudyHours  average study hours per week
     * @param accuracyRate      overall accuracy rate as a fraction 0.0–1.0
     * @param missedSessions    number of missed study sessions this week
     * @return AI prediction, or {@code null} if the sidecar is unavailable
     */
    AiRiskPrediction predictRisk(String studentId, BigDecimal ersScore,
                                  BigDecimal attendanceRate, BigDecimal weeklyStudyHours,
                                  BigDecimal accuracyRate, int missedSessions);

    record AiRiskPrediction(BigDecimal riskScore, String riskLevel,
                             List<String> factors, String recommendation) {}
}
