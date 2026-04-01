package com.edutech.chat.infrastructure.adapter.out.kafka;

import com.edutech.chat.domain.model.ProactiveNudge;
import com.edutech.chat.domain.port.out.ChatEventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
    public void publishMessageSent(UUID sessionId, UUID userId, int tokenCount, int latencyMs) {
        publish(chatEventsTopic, Map.of(
            "eventType", "CHAT_MESSAGE_SENT",
            "sessionId", sessionId.toString(),
            "userId", userId.toString(),
            "tokenCount", tokenCount,
            "latencyMs", latencyMs,
            "occurredAt", Instant.now().toString()
        ));
    }

    @Override
    public void publishNudgeNotification(ProactiveNudge nudge) {
        publish(notificationSendTopic, Map.of(
            "recipientId", nudge.getUserId().toString(),
            "subject", "NexusChat has insights for you",
            "body", nudge.getMessage(),
            "channel", "IN_APP",
            "notificationType", "NEXUS_CHAT_NUDGE",
            "actionUrl", nudge.getActionUrl() != null ? nudge.getActionUrl() : "/chat",
            "occurredAt", Instant.now().toString()
        ));
    }

    public void publishSessionStarted(UUID sessionId, UUID userId) {
        publish(chatEventsTopic, Map.of(
            "eventType", "CHAT_SESSION_STARTED",
            "sessionId", sessionId.toString(),
            "userId", userId.toString(),
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
