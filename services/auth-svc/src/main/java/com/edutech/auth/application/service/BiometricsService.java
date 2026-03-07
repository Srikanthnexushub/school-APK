// src/main/java/com/edutech/auth/application/service/BiometricsService.java
package com.edutech.auth.application.service;

import com.edutech.auth.application.dto.BiometricsRequest;
import com.edutech.auth.application.dto.BiometricsRiskScore;
import com.edutech.auth.application.dto.KeystrokeEvent;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Keystroke dynamics risk scorer.
 *
 * Computes a heuristic risk score based on inter-key timing variance.
 * Low coefficient of variation → consistent, human-like typing → low risk.
 * High coefficient of variation → irregular timing → potential bot or anomaly.
 *
 * Production upgrade: replace this heuristic with the ML model endpoint
 * in ai-gateway-svc once the Python sidecar is trained.
 */
@Service
public class BiometricsService {

    public BiometricsRiskScore calculateRisk(BiometricsRequest request) {
        List<KeystrokeEvent> keystrokes = request.keystrokes();

        if (keystrokes.size() < 2) {
            // Insufficient data — return neutral score
            return BiometricsRiskScore.of(0.5, request.sessionId());
        }

        double[] holdTimes = keystrokes.stream()
            .mapToDouble(KeystrokeEvent::holdDuration)
            .toArray();

        double mean = mean(holdTimes);
        double stdDev = stdDev(holdTimes, mean);

        // Coefficient of variation (normalized std dev)
        double cv = mean > 0 ? stdDev / mean : 1.0;

        // Clamp to [0, 1]
        double score = Math.min(1.0, Math.max(0.0, cv));

        return BiometricsRiskScore.of(score, request.sessionId());
    }

    private double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private double stdDev(double[] values, double mean) {
        double sumSq = 0;
        for (double v : values) sumSq += Math.pow(v - mean, 2);
        return Math.sqrt(sumSq / values.length);
    }
}
