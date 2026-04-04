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
public class ExamTrackerWebClientAdapter {

    private final WebClient webClient;
    private final long timeoutMs;

    public ExamTrackerWebClientAdapter(
            WebClient.Builder builder,
            @Value("${downstream.exam-tracker.uri}") String uri,
            @Value("${downstream.exam-tracker.timeout-ms:500}") long timeoutMs) {
        this.webClient = builder.baseUrl(uri).build();
        this.timeoutMs = timeoutMs;
    }

    public Mono<ExamVelocityDto> fetchVelocity(UUID studentId, String jwt) {
        return webClient.get()
            .uri("/api/v1/exam-tracker/students/{studentId}/velocity", studentId)
            .header("Authorization", "Bearer " + jwt)
            .retrieve()
            .bodyToMono(ExamVelocityDto.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorReturn(ExamVelocityDto.empty());
    }
}
