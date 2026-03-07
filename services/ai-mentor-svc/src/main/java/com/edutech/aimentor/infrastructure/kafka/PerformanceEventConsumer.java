package com.edutech.aimentor.infrastructure.kafka;

import com.edutech.aimentor.application.service.RecommendationService;
import com.edutech.aimentor.domain.model.PriorityLevel;
import com.edutech.aimentor.domain.model.SubjectArea;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes performance.weak.area.detected events from the performance/assessment service
 * and triggers recommendation creation for the affected student.
 *
 * Expected payload JSON fields:
 *   studentId, enrollmentId, subjectArea, topic, priorityLevel
 */
@Component
public class PerformanceEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PerformanceEventConsumer.class);

    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;

    public PerformanceEventConsumer(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "performance.weak.area.detected", groupId = "ai-mentor-svc")
    public void onWeakAreaDetected(String message) {
        log.debug("Received performance.weak.area.detected event: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);

            UUID studentId = UUID.fromString(node.get("studentId").asText());
            UUID enrollmentId = UUID.fromString(node.get("enrollmentId").asText());
            SubjectArea subjectArea = SubjectArea.valueOf(node.get("subjectArea").asText());
            String topic = node.get("topic").asText();

            PriorityLevel priorityLevel = PriorityLevel.MEDIUM;
            if (node.has("priorityLevel")) {
                try {
                    priorityLevel = PriorityLevel.valueOf(node.get("priorityLevel").asText());
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown priorityLevel value, defaulting to MEDIUM: {}", node.get("priorityLevel").asText());
                }
            }

            String recommendationText = buildRecommendationText(subjectArea, topic);

            recommendationService.createRecommendation(
                    studentId, enrollmentId, subjectArea, topic, recommendationText, priorityLevel
            );

            log.info("Recommendation created from weak area event: studentId={} subjectArea={} topic={}",
                    studentId, subjectArea, topic);

        } catch (Exception e) {
            log.error("Failed to process performance.weak.area.detected event: {} error={}",
                    message, e.getMessage(), e);
            // Do not re-throw — allow Kafka to continue processing subsequent messages
        }
    }

    private String buildRecommendationText(SubjectArea subjectArea, String topic) {
        return "Based on your recent performance, we recommend dedicating additional study time to "
                + topic + " in " + subjectArea.name().toLowerCase().replace('_', ' ')
                + ". Review fundamentals, practise problems, and use the spaced repetition plan for best results.";
    }
}
