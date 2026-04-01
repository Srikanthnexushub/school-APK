# NexusChat — Infrastructure Layer (Full Code)

## ChatController.java

```java
package com.edutech.chat.infrastructure.adapter.in;

import com.edutech.chat.application.service.ChatSessionService;
import com.edutech.chat.domain.model.ChatSession;
import com.edutech.chat.infrastructure.adapter.in.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatSessionService chatSessionService;

    /**
     * Start a new chat session. Returns greeting + sessionId.
     */
    @PostMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StartSessionResponse> startSession(
            @RequestBody @Valid StartSessionRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("Authorization") String authHeader) {

        String jwt = authHeader.replace("Bearer ", "");
        var result = chatSessionService.startSession(userId, userRole,
            request.pageContext(), jwt);

        return ResponseEntity.ok(new StartSessionResponse(
            result.sessionId(),
            result.greeting(),
            result.context().fullName().split(" ")[0]
        ));
    }

    /**
     * Stream a message response (SSE token-by-token).
     * Returns text/event-stream — each line is a JSON ChatTokenEvent.
     */
    @PostMapping(value = "/sessions/{sessionId}/stream",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseBodyEmitter streamMessage(
            @PathVariable UUID sessionId,
            @RequestBody @Valid StreamMessageRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Authorization") String authHeader) {

        String jwt = authHeader.replace("Bearer ", "");
        return chatSessionService.streamMessage(sessionId, userId,
            request.message(), request.pageContext(), jwt);
    }

    /**
     * Non-streaming fallback (used when SSE is unavailable).
     */
    @PostMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SendMessageResponse> sendMessage(
            @PathVariable UUID sessionId,
            @RequestBody @Valid SendMessageRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Authorization") String authHeader) {

        String jwt = authHeader.replace("Bearer ", "");
        var result = chatSessionService.sendMessage(sessionId, userId,
            request.message(), request.pageContext(), jwt);
        return ResponseEntity.ok(result);
    }

    /**
     * List all sessions for the current user (history sidebar).
     */
    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatSessionSummaryDto>> getSessions(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(chatSessionService.getSessionSummaries(userId));
    }

    /**
     * Get all messages in a session.
     */
    @GetMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(chatSessionService.getMessages(sessionId, userId));
    }

    /**
     * Delete (archive) a session.
     */
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId) {
        chatSessionService.deleteSession(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get pending proactive nudges for current user.
     */
    @GetMapping("/nudges/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProactiveNudgeDto>> getPendingNudges(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(chatSessionService.getPendingNudges(userId));
    }

    /**
     * Mark nudge as opened.
     */
    @PutMapping("/nudges/{nudgeId}/open")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> openNudge(
            @PathVariable UUID nudgeId,
            @RequestHeader("X-User-Id") UUID userId) {
        chatSessionService.markNudgeOpened(nudgeId, userId);
        return ResponseEntity.ok().build();
    }
}
```

## Request/Response DTOs

```java
// StartSessionRequest.java
package com.edutech.chat.infrastructure.adapter.in.dto;
import jakarta.validation.constraints.Size;
public record StartSessionRequest(
    @Size(max = 100) String pageContext   // nullable — "dashboard", "performance", etc.
) {}

// StartSessionResponse.java
public record StartSessionResponse(
    java.util.UUID sessionId,
    String greeting,
    String firstName
) {}

// StreamMessageRequest.java
public record StreamMessageRequest(
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 4000)
    String message,
    @Size(max = 100) String pageContext
) {}

// SendMessageRequest.java
public record SendMessageRequest(
    @jakarta.validation.constraints.NotBlank String message,
    String pageContext
) {}

// SendMessageResponse.java
public record SendMessageResponse(
    java.util.UUID messageId,
    String content,
    String actionJson   // nullable
) {}

// ChatSessionSummaryDto.java
public record ChatSessionSummaryDto(
    java.util.UUID id,
    String title,
    String lastMessage,
    int messageCount,
    java.time.Instant lastActiveAt
) {}

// ChatMessageDto.java
public record ChatMessageDto(
    java.util.UUID id,
    String role,
    String content,
    String messageType,
    java.util.Map<String, Object> actionPayload,
    java.time.Instant createdAt
) {}

// ProactiveNudgeDto.java
public record ProactiveNudgeDto(
    java.util.UUID id,
    String message,
    String actionUrl,
    String triggerType,
    java.time.Instant createdAt
) {}
```

## JpaChatSessionRepository.java

```java
package com.edutech.chat.infrastructure.adapter.out.jpa;

import com.edutech.chat.domain.model.ChatSession;
import com.edutech.chat.domain.model.SessionStatus;
import com.edutech.chat.domain.port.out.ChatSessionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaChatSessionRepository
    extends JpaRepository<ChatSession, UUID>, ChatSessionRepository {

    @Override
    @Query("SELECT s FROM ChatSession s WHERE s.id = :id AND s.userId = :userId AND s.deletedAt IS NULL")
    Optional<ChatSession> findByIdAndUserId(UUID id, UUID userId);

    @Override
    @Query("SELECT s FROM ChatSession s WHERE s.userId = :userId AND s.status = 'ACTIVE' " +
           "AND s.deletedAt IS NULL ORDER BY s.lastActiveAt DESC")
    List<ChatSession> findActiveByUserId(UUID userId);
}
```

## JpaChatMessageRepository.java

```java
package com.edutech.chat.infrastructure.adapter.out.jpa;

import com.edutech.chat.domain.model.ChatMessage;
import com.edutech.chat.domain.port.out.ChatMessageRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaChatMessageRepository
    extends JpaRepository<ChatMessage, UUID>, ChatMessageRepository {

    @Override
    @Query("SELECT m FROM ChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt ASC")
    List<ChatMessage> findAllBySessionId(UUID sessionId);

    @Override
    @Query(value = "SELECT * FROM chat_schema.chat_messages WHERE session_id = :sessionId " +
                   "ORDER BY created_at DESC LIMIT :n", nativeQuery = true)
    List<ChatMessage> findLastNBySessionId(UUID sessionId, int n);
}
```

## AiGatewayStreamWebClientAdapter.java

```java
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
            "systemPrompt", systemPrompt,
            "history", history,
            "userMessage", userMessage,
            "maxTokens", 1200,
            "temperature", 0.7,
            "stream", true
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
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if (!"[DONE]".equals(data)) {
                            String token = extractTokenFromDelta(data);
                            if (token != null && !token.isEmpty()) {
                                outputTokens[0]++;
                                consumer.onToken(token);
                            }
                        }
                    }
                })
                .doOnComplete(() -> consumer.onComplete(
                    estimateInputTokens(systemPrompt, history, userMessage),
                    outputTokens[0],
                    (int)(System.currentTimeMillis() - startMs)
                ))
                .doOnError(consumer::onError)
                .blockLast();  // blocks the stream thread (not main thread)

        } catch (Exception e) {
            log.error("AI Gateway stream failed: {}", e.getMessage());
            // Fallback: try non-streaming endpoint
            fallbackToBlocking(systemPrompt, userMessage, consumer, startMs);
        }
    }

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
                // Simulate streaming by splitting into words
                for (String word : content.split(" ")) {
                    consumer.onToken(word + " ");
                }
                consumer.onComplete(0, 0, (int)(System.currentTimeMillis() - startMs));
            }
        } catch (Exception ex) {
            log.error("Fallback also failed: {}", ex.getMessage());
            consumer.onError(ex);
        }
    }

    private String extractTokenFromDelta(String jsonLine) {
        // Parses: {"choices":[{"delta":{"content":"word"}}]}
        try {
            int idx = jsonLine.indexOf("\"content\":\"");
            if (idx == -1) return null;
            int start = idx + 11;
            int end = jsonLine.indexOf("\"", start);
            if (end == -1) return null;
            return jsonLine.substring(start, end)
                .replace("\\n", "\n").replace("\\\"", "\"");
        } catch (Exception e) {
            return null;
        }
    }

    private int estimateInputTokens(String systemPrompt, List<Map<String, String>> history,
                                     String userMessage) {
        // ~4 chars per token estimation
        int chars = systemPrompt.length() + userMessage.length();
        for (var msg : history) chars += msg.getOrDefault("content", "").length();
        return chars / 4;
    }
}
```

## WebClientConfig.java

```java
package com.edutech.chat.infrastructure.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder(
            @Value("${downstream.ai-gateway.timeout-ms:30000}") int timeoutMs) {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofMillis(timeoutMs));
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
```

## StreamThreadPoolConfig.java

```java
package com.edutech.chat.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class StreamThreadPoolConfig {

    @Bean("streamExecutor")
    public ExecutorService streamExecutor(
            @Value("${chat.stream-thread-pool-size:10}") int poolSize) {
        return Executors.newFixedThreadPool(poolSize,
            r -> {
                Thread t = new Thread(r, "chat-stream-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            });
    }
}
```

## PlatformEventKafkaConsumer.java

```java
package com.edutech.chat.infrastructure.adapter.in.kafka;

import com.edutech.chat.application.service.ProactiveNudgeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlatformEventKafkaConsumer {

    private final ProactiveNudgeService nudgeService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = {"${kafka.topics.assess-events}"},
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onAssessEvent(String eventJson) {
        try {
            JsonNode node = objectMapper.readTree(eventJson);
            String eventType = node.path("eventType").asText("");

            if ("EXAM_SUBMITTED".equals(eventType) || "ExamSubmittedEvent".equals(eventType)) {
                UUID studentId = UUID.fromString(node.path("studentId").asText());
                String examTitle = node.path("examTitle").asText("Exam");
                double percentage = node.path("percentage").asDouble(0);
                int wrongCount = node.path("wrongCount").asInt(0);
                UUID examId = UUID.fromString(node.path("examId").asText());

                nudgeService.handleExamSubmitted(studentId, examTitle, percentage, wrongCount, examId);
            }
        } catch (Exception e) {
            log.warn("Failed to process assess-event for nudge: {}", e.getMessage());
        }
    }

    @KafkaListener(
        topics = {"${kafka.topics.performance-events}"},
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onPerformanceEvent(String eventJson) {
        try {
            JsonNode node = objectMapper.readTree(eventJson);
            String eventType = node.path("eventType").asText("");

            if ("WEAK_AREA_DETECTED".equals(eventType)) {
                String severity = node.path("severity").asText("LOW");
                if ("CRITICAL".equals(severity)) {
                    UUID studentId = UUID.fromString(node.path("studentId").asText());
                    String subject = node.path("subject").asText("Unknown Subject");
                    String topic = node.path("topicName").asText("Unknown Topic");
                    nudgeService.handleWeakAreaCritical(studentId, subject, topic);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process performance-event for nudge: {}", e.getMessage());
        }
    }
}
```

## KafkaChatEventPublisher.java

```java
package com.edutech.chat.infrastructure.adapter.out.kafka;

import com.edutech.chat.domain.port.out.ChatEventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaChatEventPublisher implements ChatEventPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.nexus-chat-events}")
    private String chatEventsTopic;

    @Value("${kafka.topics.notification-send}")
    private String notificationSendTopic;

    @Override
    public void publishSessionStarted(UUID sessionId, UUID userId) {
        publish(chatEventsTopic, Map.of(
            "eventType", "CHAT_SESSION_STARTED",
            "sessionId", sessionId.toString(),
            "userId", userId.toString(),
            "occurredAt", Instant.now().toString()
        ));
    }

    @Override
    public void publishNotification(UUID recipientId, String subject, String body,
                                     String notificationType, String actionUrl) {
        publish(notificationSendTopic, Map.of(
            "recipientId", recipientId.toString(),
            "subject", subject,
            "body", body,
            "channel", "IN_APP",
            "notificationType", notificationType,
            "actionUrl", actionUrl,
            "occurredAt", Instant.now().toString()
        ));
    }

    private void publish(String topic, Map<String, Object> payload) {
        try {
            kafkaTemplate.send(topic, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to publish to topic {}: {}", topic, e.getMessage());
        }
    }
}
```

## SecurityConfig.java

```java
package com.edutech.chat.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        return http.build();
    }
}
```
