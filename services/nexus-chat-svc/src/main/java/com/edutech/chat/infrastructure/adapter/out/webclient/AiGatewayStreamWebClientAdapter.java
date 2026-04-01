package com.edutech.chat.infrastructure.adapter.out.webclient;

import com.edutech.chat.domain.port.out.AiGatewayStreamPort;
import com.edutech.chat.domain.port.out.StreamTokenConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AiGatewayStreamWebClientAdapter implements AiGatewayStreamPort {

    private final WebClient webClient;
    private final String serviceApiKey;

    public AiGatewayStreamWebClientAdapter(
            WebClient.Builder builder,
            @Value("${downstream.ai-gateway.uri}") String aiGatewayUri,
            @Value("${service.api-key}") String serviceApiKey) {
        this.webClient = builder.baseUrl(aiGatewayUri).build();
        this.serviceApiKey = serviceApiKey;
    }

    @Override
    public void streamCompletion(String systemPrompt, List<Map<String, String>> history,
                                  String userMessage, StreamTokenConsumer consumer) {
        var requestBody = Map.of(
            "requesterId", "nexus-chat-svc",
            "systemPrompt", systemPrompt,
            "userMessage", userMessage,
            "maxTokens", 1200,
            "temperature", 0.7
        );

        long startMs = System.currentTimeMillis();
        final int[] outputTokens = {0};

        try {
            webClient.post()
                .uri("/api/v1/ai/completions/stream")
                .header("X-Service-Key", serviceApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(line -> {
                    if (line == null || line.isBlank()) return;
                    // Spring's SSE decoder strips "data: " prefix — content arrives raw.
                    // Strip it defensively in case response is consumed as text/plain.
                    String data = line.startsWith("data: ") ? line.substring(6).trim() : line.trim();
                    if (!"[DONE]".equals(data)) {
                        String token = extractTokenFromDelta(data);
                        if (token != null && !token.isEmpty()) {
                            outputTokens[0]++;
                            consumer.onToken(token);
                        }
                    }
                })
                .doOnComplete(() -> consumer.onComplete(
                    estimateInputTokens(systemPrompt, history, userMessage),
                    outputTokens[0],
                    (int)(System.currentTimeMillis() - startMs)
                ))
                .doOnError(consumer::onError)
                .blockLast();

        } catch (Exception e) {
            log.error("AI Gateway stream failed: {}", e.getMessage());
            fallbackToBlocking(systemPrompt, userMessage, consumer, startMs);
        }
    }

    @SuppressWarnings("unchecked")
    private void fallbackToBlocking(String systemPrompt, String userMessage,
                                     StreamTokenConsumer consumer, long startMs) {
        try {
            var response = webClient.post()
                .uri("/api/v1/ai/completions")
                .header("X-Service-Key", serviceApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                    "systemPrompt", systemPrompt,
                    "userMessage", userMessage,
                    "maxTokens", 1200,
                    "temperature", 0.7
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.get("content") != null) {
                String content = response.get("content").toString();
                for (String word : content.split(" ")) {
                    consumer.onToken(word + " ");
                }
                consumer.onComplete(0, 0, (int)(System.currentTimeMillis() - startMs));
            } else {
                consumer.onError(new RuntimeException("Empty response from AI gateway"));
            }
        } catch (Exception ex) {
            log.error("AI Gateway fallback also failed: {}", ex.getMessage());
            consumer.onError(ex);
        }
    }

    private String extractTokenFromDelta(String jsonLine) {
        try {
            int idx = jsonLine.indexOf("\"content\":\"");
            if (idx == -1) return null;
            int start = idx + 11;
            int end = jsonLine.indexOf("\"", start);
            if (end == -1) return null;
            return jsonLine.substring(start, end)
                .replace("\\n", "\n").replace("\\\"", "\"").replace("\\t", "\t");
        } catch (Exception e) {
            return null;
        }
    }

    private int estimateInputTokens(String systemPrompt, List<Map<String, String>> history,
                                     String userMessage) {
        int chars = systemPrompt.length() + userMessage.length();
        for (var msg : history) {
            String content = msg.getOrDefault("content", "");
            chars += content.length();
        }
        return chars / 4;
    }
}
