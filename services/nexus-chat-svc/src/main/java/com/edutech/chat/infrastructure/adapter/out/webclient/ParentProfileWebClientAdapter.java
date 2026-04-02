package com.edutech.chat.infrastructure.adapter.out.webclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Fetches parent profile from parent-svc.
 * Sends X-User-Id header for direct service-to-service identity resolution.
 */
@Component
@Slf4j
public class ParentProfileWebClientAdapter {

    private final WebClient webClient;
    private final long timeoutMs;

    public ParentProfileWebClientAdapter(
            WebClient.Builder builder,
            @Value("${downstream.parent.uri}") String uri,
            @Value("${downstream.parent.timeout-ms:500}") long timeoutMs) {
        this.webClient = builder.baseUrl(uri).build();
        this.timeoutMs = timeoutMs;
    }

    public Mono<ParentProfileDto> fetchProfile(UUID userId, String jwt) {
        return webClient.get()
            .uri("/api/v1/parents/me")
            .header("Authorization", "Bearer " + jwt)
            .header("X-User-Id", userId.toString())
            .retrieve()
            .bodyToMono(ParentProfileDto.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorReturn(ParentProfileDto.empty());
    }
}
