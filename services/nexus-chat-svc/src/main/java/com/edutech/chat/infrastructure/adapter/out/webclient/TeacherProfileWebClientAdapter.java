package com.edutech.chat.infrastructure.adapter.out.webclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Fetches teacher profile from mentor-svc.
 * Sends X-User-Id header because mentor-svc resolves identity from it (normally injected by gateway).
 */
@Component
@Slf4j
public class TeacherProfileWebClientAdapter {

    private final WebClient webClient;
    private final long timeoutMs;

    public TeacherProfileWebClientAdapter(
            WebClient.Builder builder,
            @Value("${downstream.mentor.uri}") String uri,
            @Value("${downstream.mentor.timeout-ms:500}") long timeoutMs) {
        this.webClient = builder.baseUrl(uri).build();
        this.timeoutMs = timeoutMs;
    }

    public Mono<TeacherProfileDto> fetchProfile(UUID userId, String jwt) {
        return webClient.get()
            .uri("/api/v1/mentors/me")
            .header("Authorization", "Bearer " + jwt)
            .header("X-User-Id", userId.toString())
            .retrieve()
            .bodyToMono(TeacherProfileDto.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorReturn(TeacherProfileDto.empty());
    }
}
