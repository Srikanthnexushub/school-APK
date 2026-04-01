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
public class PerformanceWebClientAdapter {

    private final WebClient webClient;
    private final long timeoutMs;

    public PerformanceWebClientAdapter(
            WebClient.Builder builder,
            @Value("${downstream.performance.uri}") String uri,
            @Value("${downstream.performance.timeout-ms:500}") long timeoutMs) {
        this.webClient = builder.baseUrl(uri).build();
        this.timeoutMs = timeoutMs;
    }

    public Mono<PerformanceDto> fetchPerformance(UUID userId, String jwt) {
        return webClient.get()
            .uri("/api/v1/performance/{userId}/chat-context", userId)
            .header("Authorization", "Bearer " + jwt)
            .retrieve()
            .bodyToMono(PerformanceDto.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorReturn(PerformanceDto.empty());
    }
}
