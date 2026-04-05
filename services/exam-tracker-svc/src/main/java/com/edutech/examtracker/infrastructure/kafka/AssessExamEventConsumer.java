package com.edutech.examtracker.infrastructure.kafka;

import com.edutech.examtracker.application.service.ExamEnrollmentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Consumes exam completion events from assess-svc and auto-syncs the student's
 * exam record into exam-tracker-svc.
 *
 * <p>assess-svc publishes two event types to the {@code assess-events} topic:
 * <ul>
 *   <li>{@code ExamSubmittedEvent} — fired when a student submits answers</li>
 *   <li>{@code GradeIssuedEvent}  — fired after grading is complete</li>
 * </ul>
 *
 * <p>This consumer handles {@code GradeIssuedEvent} (identified by the presence of
 * the {@code gradeId} field) because it contains the final score and centerId needed
 * for a meaningful enrollment record. Unknown event shapes are silently skipped.
 *
 * <p>Fail-safe contract: any exception during processing is caught, logged, and
 * swallowed — the Kafka offset is committed and the consumer stays healthy.
 */
@Component
public class AssessExamEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AssessExamEventConsumer.class);

    private final ExamEnrollmentService enrollmentService;
    private final ObjectMapper objectMapper;

    public AssessExamEventConsumer(ExamEnrollmentService enrollmentService,
                                   ObjectMapper objectMapper) {
        this.enrollmentService = enrollmentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "${kafka.topics.assess-events}",
        groupId = "exam-tracker-assess-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAssessEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            // Only process GradeIssuedEvent — it has the gradeId field and final score.
            // ExamSubmittedEvent is also on this topic; skip it gracefully.
            if (!root.has("gradeId")) {
                log.debug("exam-tracker consumer: skipping non-GradeIssuedEvent message");
                return;
            }

            UUID studentId = parseUuid(root, "studentId");
            UUID examId    = parseUuid(root, "examId");

            if (studentId == null || examId == null) {
                log.warn("exam-tracker consumer: GradeIssuedEvent missing studentId or examId — skipping. raw={}",
                         message.length() > 200 ? message.substring(0, 200) : message);
                return;
            }

            // Derive exam year from occurredAt (or fall back to current year)
            int examYear = LocalDate.now().getYear();
            if (root.has("occurredAt") && !root.get("occurredAt").isNull()) {
                try {
                    examYear = Instant.parse(root.get("occurredAt").asText())
                                     .atZone(java.time.ZoneOffset.UTC)
                                     .getYear();
                } catch (Exception ignored) {
                    // fallback to current year
                }
            }

            // Use a descriptive name; prefer the examId string if no title is present
            // (assess-svc GradeIssuedEvent does not carry examTitle — use examId as name)
            String examName = "School Exam " + examId;

            enrollmentService.syncFromAssessEvent(studentId, examId, examName, examYear);

        } catch (Exception e) {
            log.error("exam-tracker consumer: failed to process assess-events message — skipping. error={}",
                      e.getMessage(), e);
        }
    }

    private UUID parseUuid(JsonNode node, String field) {
        try {
            JsonNode val = node.get(field);
            return (val != null && !val.isNull()) ? UUID.fromString(val.asText()) : null;
        } catch (IllegalArgumentException e) {
            log.warn("exam-tracker consumer: could not parse UUID for field={} value={}",
                     field, node.has(field) ? node.get(field).asText() : "missing");
            return null;
        }
    }
}
