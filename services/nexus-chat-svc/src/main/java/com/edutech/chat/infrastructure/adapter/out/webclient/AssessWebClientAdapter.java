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
public class AssessWebClientAdapter {

    private final WebClient webClient;
    private final long timeoutMs;

    public AssessWebClientAdapter(
            WebClient.Builder builder,
            @Value("${downstream.assess.uri}") String uri,
            @Value("${downstream.assess.timeout-ms:500}") long timeoutMs) {
        this.webClient = builder.baseUrl(uri).build();
        this.timeoutMs = timeoutMs;
    }

    public Mono<AssessContextDto> fetchAssessContext(UUID userId, String jwt) {
        return webClient.get()
            .uri("/api/v1/assessments/students/{userId}/chat-context", userId)
            .header("Authorization", "Bearer " + jwt)
            .retrieve()
            .bodyToMono(AssessContextDto.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorReturn(AssessContextDto.empty());
    }
}
