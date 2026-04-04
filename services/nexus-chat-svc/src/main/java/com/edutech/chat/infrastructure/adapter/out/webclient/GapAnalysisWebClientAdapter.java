package com.edutech.chat.infrastructure.adapter.out.webclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Component
@Slf4j
public class GapAnalysisWebClientAdapter {

    private final WebClient webClient;
    private final long timeoutMs;

    public GapAnalysisWebClientAdapter(
            WebClient.Builder builder,
            @Value("${downstream.performance.uri}") String performanceUri,
            @Value("${downstream.performance.timeout-ms:500}") long timeoutMs) {
        this.webClient = builder.baseUrl(performanceUri).build();
        this.timeoutMs = timeoutMs;
    }

    public Mono<GapAnalysisDto> fetchGapAnalysis(UUID studentId, String jwt) {
        return webClient.get()
            .uri("/api/v1/performance/gap-analysis/{studentId}", studentId)
            .header("Authorization", "Bearer " + jwt)
            .header("X-User-Id", studentId.toString())
            .header("X-User-Role", "STUDENT")
            .retrieve()
            .bodyToMono(GapAnalysisDto.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorReturn(GapAnalysisDto.empty());
    }
}
