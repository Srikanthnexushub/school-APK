package com.edutech.performance.infrastructure.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes events from student-profile-svc.
 * Handles AcademicRecordAddedEvent and other student lifecycle events.
 */
@Component
public class StudentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(StudentEventConsumer.class);

    @KafkaListener(
            topics = "${kafka.topics.student-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleStudentEvent(@Payload Map<String, Object> payload) {
        String eventType = String.valueOf(payload.getOrDefault("eventType", "UNKNOWN"));
        log.info("Received student event of type: {}", eventType);

        switch (eventType) {
            case "AcademicRecordAddedEvent" -> handleAcademicRecordAdded(payload);
            default -> log.debug("Ignoring unrecognised student event type: {}", eventType);
        }
    }

    private void handleAcademicRecordAdded(Map<String, Object> payload) {
        log.info("AcademicRecordAddedEvent received for studentId={} — storing for future syllabus baseline (Phase 2)",
                payload.get("studentId"));
        // Phase 2: store academic record to establish syllabus baseline for ERS computation
    }
}
