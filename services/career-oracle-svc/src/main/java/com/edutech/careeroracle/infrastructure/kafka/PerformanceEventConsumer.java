package com.edutech.careeroracle.infrastructure.kafka;

import com.edutech.careeroracle.domain.port.in.GenerateCareerRecommendationsUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class PerformanceEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PerformanceEventConsumer.class);

    private final GenerateCareerRecommendationsUseCase generateCareerRecommendationsUseCase;
    private final ObjectMapper objectMapper;

    public PerformanceEventConsumer(GenerateCareerRecommendationsUseCase generateCareerRecommendationsUseCase) {
        this.generateCareerRecommendationsUseCase = generateCareerRecommendationsUseCase;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "performance.readiness.updated", groupId = "career-oracle-svc")
    public void handlePerformanceReadinessUpdated(String message) {
        log.info("Received performance.readiness.updated event: {}", message);
        try {
            JsonNode root = objectMapper.readTree(message);

            JsonNode studentIdNode = root.get("studentId");
            if (studentIdNode == null || studentIdNode.isNull()) {
                log.warn("Received performance.readiness.updated event without studentId, skipping");
                return;
            }

            UUID studentId = UUID.fromString(studentIdNode.asText());

            log.info("Triggering recommendation refresh for studentId={}", studentId);
            generateCareerRecommendationsUseCase.generateRecommendations(studentId, Map.of());
            log.info("Successfully refreshed recommendations for studentId={}", studentId);

        } catch (Exception ex) {
            log.error("Failed to process performance.readiness.updated event: {}", ex.getMessage(), ex);
        }
    }
}
