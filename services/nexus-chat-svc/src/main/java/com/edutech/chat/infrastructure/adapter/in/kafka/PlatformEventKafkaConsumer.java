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
                String examIdStr = node.path("examId").asText("");
                if (examIdStr.isBlank()) return;
                UUID examId = UUID.fromString(examIdStr);

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
