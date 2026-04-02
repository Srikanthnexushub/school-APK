package com.edutech.curricular.infrastructure.adapter.out.webclient;

import com.edutech.curricular.domain.port.out.MasteryDataPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class PerformanceMasteryAdapter implements MasteryDataPort {

    private final WebClient webClient;
    private final long timeoutMs;

    public PerformanceMasteryAdapter(WebClient.Builder builder,
            @Value("${downstream.performance.uri}") String uri,
            @Value("${downstream.performance.timeout-ms:500}") long timeoutMs) {
        this.webClient = builder.baseUrl(uri).build();
        this.timeoutMs = timeoutMs;
    }

    @Override
    public Map<String, Double> fetchMastery(UUID studentId, UUID subjectId, String jwt) {
        try {
            MasteryDto result = webClient.get()
                .uri("/api/v1/performance/student/{studentId}/mastery?subjectId={subjectId}",
                    studentId, subjectId)
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .bodyToMono(MasteryDto.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .onErrorReturn(MasteryDto.empty())
                .block();
            return result != null ? result.topicMasteryRatios() : Map.of();
        } catch (Exception e) {
            log.warn("Failed to fetch mastery data for student {} subject {}: {}", studentId, subjectId, e.getMessage());
            return Map.of();
        }
    }
}
