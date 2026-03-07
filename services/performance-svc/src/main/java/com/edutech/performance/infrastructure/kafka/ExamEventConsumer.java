package com.edutech.performance.infrastructure.kafka;

import com.edutech.performance.domain.port.in.ComputeReadinessScoreUseCase;
import com.edutech.performance.domain.port.in.UpdateSubjectMasteryUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes events from exam-tracker-svc.
 * Handles MockTestCompletedEvent and StudySessionRecordedEvent (dispatched by event type field).
 */
@Component
public class ExamEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExamEventConsumer.class);

    private final UpdateSubjectMasteryUseCase updateSubjectMasteryUseCase;
    private final ComputeReadinessScoreUseCase computeReadinessScoreUseCase;

    public ExamEventConsumer(UpdateSubjectMasteryUseCase updateSubjectMasteryUseCase,
                              ComputeReadinessScoreUseCase computeReadinessScoreUseCase) {
        this.updateSubjectMasteryUseCase = updateSubjectMasteryUseCase;
        this.computeReadinessScoreUseCase = computeReadinessScoreUseCase;
    }

    @KafkaListener(
            topics = "${kafka.topics.exam-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleExamEvent(@Payload Map<String, Object> payload) {
        String eventType = String.valueOf(payload.getOrDefault("eventType", "UNKNOWN"));
        log.info("Received exam event of type: {}", eventType);

        switch (eventType) {
            case "MockTestCompletedEvent" -> handleMockTestCompleted(payload);
            case "StudySessionRecordedEvent" -> handleStudySessionRecorded(payload);
            default -> log.debug("Ignoring unrecognised exam event type: {}", eventType);
        }
    }

    private void handleMockTestCompleted(Map<String, Object> payload) {
        try {
            UUID studentId = UUID.fromString(String.valueOf(payload.get("studentId")));
            UUID enrollmentId = UUID.fromString(String.valueOf(payload.get("enrollmentId")));
            String subject = String.valueOf(payload.getOrDefault("subject", "General"));
            double masteryPct = Double.parseDouble(String.valueOf(payload.getOrDefault("masteryPercent", "0")));

            log.info("Processing MockTestCompleted for studentId={} subject={}", studentId, subject);

            updateSubjectMasteryUseCase.updateMastery(
                    studentId, enrollmentId, subject, BigDecimal.valueOf(masteryPct));

            computeReadinessScoreUseCase.computeScore(studentId, enrollmentId);
        } catch (Exception ex) {
            log.error("Failed to process MockTestCompletedEvent: {}", ex.getMessage(), ex);
        }
    }

    private void handleStudySessionRecorded(Map<String, Object> payload) {
        try {
            UUID studentId = UUID.fromString(String.valueOf(payload.get("studentId")));
            log.info("Processing StudySessionRecorded for studentId={} — lightweight snapshot will be taken on next ERS computation", studentId);
        } catch (Exception ex) {
            log.error("Failed to process StudySessionRecordedEvent: {}", ex.getMessage(), ex);
        }
    }
}
