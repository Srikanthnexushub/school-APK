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
public class AiMentorWebClientAdapter {

    private final WebClient webClient;
    private final long timeoutMs;

    public AiMentorWebClientAdapter(
            WebClient.Builder builder,
            @Value("${downstream.ai-mentor.uri}") String uri,
            @Value("${downstream.ai-mentor.timeout-ms:500}") long timeoutMs) {
        this.webClient = builder.baseUrl(uri).build();
        this.timeoutMs = timeoutMs;
    }

    public Mono<MentorContextDto> fetchMentorContext(UUID userId, String jwt) {
        return webClient.get()
            .uri("/api/v1/mentor/students/{userId}/chat-context", userId)
            .header("Authorization", "Bearer " + jwt)
            .header("X-User-Id", userId.toString())
            .header("X-User-Role", "STUDENT")
            .retrieve()
            .bodyToMono(MentorContextDto.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorReturn(MentorContextDto.empty());
    }
}
