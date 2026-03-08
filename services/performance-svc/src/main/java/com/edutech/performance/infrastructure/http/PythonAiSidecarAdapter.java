package com.edutech.performance.infrastructure.http;

import com.edutech.performance.domain.port.out.AiDropoutRiskPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * Infrastructure adapter that calls the Python AI sidecar for dropout risk prediction.
 * Returns {@code null} on any failure so the caller can apply rule-based fallback.
 */
@Component
public class PythonAiSidecarAdapter implements AiDropoutRiskPort {

    private static final Logger log = LoggerFactory.getLogger(PythonAiSidecarAdapter.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public PythonAiSidecarAdapter(WebClient.Builder builder,
                                   @Value("${python.ai.svc.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public AiRiskPrediction predictRisk(String studentId, BigDecimal ersScore,
                                         BigDecimal attendanceRate, BigDecimal weeklyStudyHours,
                                         BigDecimal accuracyRate, int missedSessions) {

        record RiskRequest(String studentId, BigDecimal ersScore, BigDecimal attendanceRate,
                           BigDecimal weeklyStudyHours, BigDecimal accuracyRate,
                           int missedSessions) {}

        record RiskResponse(BigDecimal riskScore, String riskLevel,
                            List<String> factors, String recommendation) {}

        try {
            RiskResponse resp = webClient.post()
                    .uri("/api/v1/predict-dropout-risk")
                    .bodyValue(new RiskRequest(studentId, ersScore, attendanceRate,
                            weeklyStudyHours, accuracyRate, missedSessions))
                    .retrieve()
                    .bodyToMono(RiskResponse.class)
                    .timeout(TIMEOUT)
                    .block();

            if (resp == null) {
                throw new IllegalStateException("null response from python-ai-svc");
            }
            return new AiRiskPrediction(resp.riskScore(), resp.riskLevel(),
                    resp.factors(), resp.recommendation());

        } catch (Exception e) {
            log.warn("python-ai-svc unavailable, returning null for graceful fallback: {}",
                    e.getMessage());
            return null;
        }
    }
}
