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
public class MentorGapCoverageWebClientAdapter {

    private final WebClient webClient;
    private final long timeoutMs;

    public MentorGapCoverageWebClientAdapter(
            WebClient.Builder builder,
            @Value("${downstream.mentor.uri}") String mentorUri,
            @Value("${downstream.mentor.timeout-ms:500}") long timeoutMs) {
        this.webClient = builder.baseUrl(mentorUri).build();
        this.timeoutMs = timeoutMs;
    }

    public Mono<MentorGapCoverageDto> fetchGapCoverage(UUID studentId, String jwt) {
        return webClient.get()
            .uri("/api/v1/mentor-sessions/gap-coverage?studentId={studentId}", studentId)
            .header("Authorization", "Bearer " + jwt)
            .header("X-User-Id", studentId.toString())
            .header("X-User-Role", "STUDENT")
            .retrieve()
            .bodyToMono(MentorGapCoverageDto.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorReturn(MentorGapCoverageDto.empty());
    }
}
