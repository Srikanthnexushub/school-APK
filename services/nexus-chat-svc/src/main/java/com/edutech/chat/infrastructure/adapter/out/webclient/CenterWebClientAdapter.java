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
public class CenterWebClientAdapter {

    private final WebClient webClient;
    private final long timeoutMs;

    public CenterWebClientAdapter(
            WebClient.Builder builder,
            @Value("${downstream.center.uri}") String uri,
            @Value("${downstream.center.timeout-ms:500}") long timeoutMs) {
        this.webClient = builder.baseUrl(uri).build();
        this.timeoutMs = timeoutMs;
    }

    public Mono<CenterContextDto> fetchCenterContext(UUID userId, String jwt) {
        return webClient.get()
            .uri("/api/v1/centers/students/{userId}/chat-context", userId)
            .header("Authorization", "Bearer " + jwt)
            .retrieve()
            .bodyToMono(CenterContextDto.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorReturn(CenterContextDto.empty());
    }
}
