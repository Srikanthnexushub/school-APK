package com.edutech.aimentor.infrastructure.http;

import com.edutech.aimentor.domain.model.SubjectArea;
import com.edutech.aimentor.domain.port.out.AiGatewayClient;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * HTTP adapter that calls the ai-gateway-svc for AI-powered doubt resolution.
 *
 * Uses WebClient (injected) to POST to /api/v1/ai/completions.
 * Applies graceful degradation: .onErrorReturn({}) means the caller
 * receives an empty map when the gateway is unreachable, allowing the service
 * layer to decide the appropriate fallback behaviour.
 *
 * Per the service spec, if the AI call fails the caller (DoubtService) keeps the
 * ticket in PENDING status. "AI_UNAVAILABLE" is treated as a failure signal.
 *
 * Security: the WebClient already carries X-Service-Key (set by WebClientConfig) for
 * service-to-service authentication. In addition, the incoming user JWT is forwarded
 * in the Authorization header so ai-gateway-svc can attribute the call to the
 * originating user for audit purposes.
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

    /**
     * Extracts the Bearer token from the current servlet request.
     * Returns null if called outside a request context (e.g. async Kafka processing).
     */
    private String extractBearerToken() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String authHeader = req.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        } catch (Exception e) {
            log.debug("Could not extract Bearer token from request context: {}", e.getMessage());
        }
        return null;
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

        String jwt = extractBearerToken();

        WebClient.RequestBodySpec requestSpec = webClient.post()
                .uri("/api/v1/ai/completions");

        if (jwt != null) {
            requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }

        Map<?, ?> response = requestSpec
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
