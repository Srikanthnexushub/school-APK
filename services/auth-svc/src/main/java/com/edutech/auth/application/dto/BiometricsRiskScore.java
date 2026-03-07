// src/main/java/com/edutech/auth/application/dto/BiometricsRiskScore.java
package com.edutech.auth.application.dto;

/**
 * Immutable result of keystroke dynamics analysis.
 * Score 0.0 = very low risk (consistent human typing),
 * Score 1.0 = high risk (bot-like or anomalous behavior).
 */
public record BiometricsRiskScore(
    double score,
    String level,
    String sessionId
) {
    public static BiometricsRiskScore of(double score, String sessionId) {
        String level = score < 0.3 ? "LOW" : score < 0.7 ? "MEDIUM" : "HIGH";
        return new BiometricsRiskScore(score, level, sessionId);
    }
}
