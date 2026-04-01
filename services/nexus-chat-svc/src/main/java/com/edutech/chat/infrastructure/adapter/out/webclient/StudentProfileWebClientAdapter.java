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
public class StudentProfileWebClientAdapter {

    private final WebClient webClient;
    private final long timeoutMs;

    public StudentProfileWebClientAdapter(
            WebClient.Builder builder,
            @Value("${downstream.student-profile.uri}") String uri,
            @Value("${downstream.student-profile.timeout-ms:500}") long timeoutMs) {
        this.webClient = builder.baseUrl(uri).build();
        this.timeoutMs = timeoutMs;
    }

    public Mono<StudentProfileDto> fetchProfile(UUID userId, String jwt) {
        return webClient.get()
            .uri("/api/v1/student-profiles/{userId}/summary", userId)
            .header("Authorization", "Bearer " + jwt)
            .retrieve()
            .bodyToMono(StudentProfileDto.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorReturn(StudentProfileDto.empty(userId));
    }
}
