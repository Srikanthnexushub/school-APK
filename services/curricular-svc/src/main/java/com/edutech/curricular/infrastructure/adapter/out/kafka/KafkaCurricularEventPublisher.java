package com.edutech.curricular.infrastructure.adapter.out.kafka;

import com.edutech.curricular.domain.port.out.CurricularEventPublisher;
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
public class KafkaCurricularEventPublisher implements CurricularEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.curricular-events}")
    private String curricularEventsTopic;

    @Value("${kafka.topics.notification-send}")
    private String notificationSendTopic;

    @Override
    public void publishPathComputed(UUID studentId, UUID pathId, UUID subjectId) {
        publish(curricularEventsTopic, Map.of(
            "eventType", "LEARNING_PATH_COMPUTED",
            "studentId", studentId.toString(),
            "pathId", pathId.toString(),
            "subjectId", subjectId.toString(),
            "occurredAt", Instant.now().toString()
        ));
    }

    @Override
    public void publishPathItemCompleted(UUID studentId, UUID itemId, UUID topicId) {
        publish(curricularEventsTopic, Map.of(
            "eventType", "LEARNING_PATH_ITEM_COMPLETED",
            "studentId", studentId.toString(),
            "itemId", itemId.toString(),
            "topicId", topicId.toString(),
            "occurredAt", Instant.now().toString()
        ));
    }

    @Override
    public void publishCoverageLogged(UUID teacherId, UUID centerId, UUID topicId) {
        publish(curricularEventsTopic, Map.of(
            "eventType", "COVERAGE_LOGGED",
            "teacherId", teacherId.toString(),
            "centerId", centerId.toString(),
            "topicId", topicId.toString(),
            "occurredAt", Instant.now().toString()
        ));
    }

    @Override
    public void publishActivityEnrolled(UUID studentId, UUID activityId) {
        publish(curricularEventsTopic, Map.of(
            "eventType", "ACTIVITY_ENROLLED",
            "studentId", studentId.toString(),
            "activityId", activityId.toString(),
            "occurredAt", Instant.now().toString()
        ));
    }

    @Override
    public void publishAchievementAwarded(UUID studentId, UUID activityId, String achievementType) {
        publish(notificationSendTopic, Map.of(
            "recipientId", studentId.toString(),
            "subject", "Congratulations! You earned an achievement",
            "body", "You received a " + achievementType + " achievement for your activity participation.",
            "channel", "IN_APP",
            "notificationType", "ACHIEVEMENT_AWARDED",
            "actionUrl", "/activities",
            "occurredAt", Instant.now().toString()
        ));
        publish(curricularEventsTopic, Map.of(
            "eventType", "ACHIEVEMENT_AWARDED",
            "studentId", studentId.toString(),
            "activityId", activityId.toString(),
            "achievementType", achievementType,
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
