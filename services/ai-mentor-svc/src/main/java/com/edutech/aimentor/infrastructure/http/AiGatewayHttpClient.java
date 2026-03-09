package com.edutech.aimentor.infrastructure.http;

import com.edutech.aimentor.domain.model.SubjectArea;
import com.edutech.aimentor.domain.port.out.AiGatewayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * HTTP adapter that calls the ai-gateway-svc for AI-powered doubt resolution.
 *
 * Uses WebClient (injected) to POST to /api/v1/ai/chat.
 * Applies graceful degradation: .onErrorReturn("AI_UNAVAILABLE") means the caller
 * receives "AI_UNAVAILABLE" when the gateway is unreachable, allowing the service
 * layer to decide the appropriate fallback behaviour.
 *
 * Per the service spec, if the AI call fails the caller (DoubtService) keeps the
 * ticket in PENDING status. "AI_UNAVAILABLE" is treated as a failure signal.
 */
@Component
public class AiGatewayHttpClient implements AiGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayHttpClient.class);
    private static final String AI_UNAVAILABLE = "AI_UNAVAILABLE";

    private final WebClient webClient;
    private final int timeoutSeconds;

    public AiGatewayHttpClient(
            WebClient aiGatewayWebClient,
            @Value("${ai-gateway.timeout-seconds}") int timeoutSeconds) {
        this.webClient = aiGatewayWebClient;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String resolveDoubt(String question, SubjectArea subjectArea) {
        Map<String, Object> requestBody = Map.of(
                "requesterId", "ai-mentor-doubt",
                "systemPrompt", "You are an expert tutor. Answer the student's academic question clearly and concisely. Subject area: " + subjectArea.name(),
                "userMessage", question,
                "maxTokens", 1024,
                "temperature", 0.5
        );

        Map<?, ?> response = webClient.post()
                .uri("/api/v1/ai/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorReturn(Map.of())
                .block(Duration.ofSeconds(timeoutSeconds));

        String answer = (response != null && response.containsKey("content"))
                ? (String) response.get("content") : null;

        if (answer == null || answer.isBlank()) {
            log.warn("AI gateway returned unavailable for subjectArea={}", subjectArea);
            throw new RuntimeException("AI gateway is unavailable or returned no response");
        }

        return answer;
    }
}
