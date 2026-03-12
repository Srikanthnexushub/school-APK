// src/main/java/com/edutech/parent/infrastructure/messaging/AssessEventConsumer.java
package com.edutech.parent.infrastructure.messaging;

import com.edutech.events.notification.NotificationSendEvent;
import com.edutech.parent.domain.model.ParentProfile;
import com.edutech.parent.domain.model.StudentLink;
import com.edutech.parent.domain.port.out.ParentProfileRepository;
import com.edutech.parent.domain.port.out.StudentLinkRepository;
import com.edutech.parent.infrastructure.config.KafkaTopicProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes assess-svc domain events to fan out IN_APP notifications to parents.
 * On GradeIssuedEvent: looks up all active parents for the student and publishes
 * a RESULT_PUBLISHED notification for each parent so they know their child's result.
 */
@Component
public class AssessEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AssessEventConsumer.class);

    private final StudentLinkRepository studentLinkRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;
    private final ObjectMapper objectMapper;

    public AssessEventConsumer(StudentLinkRepository studentLinkRepository,
                                ParentProfileRepository parentProfileRepository,
                                KafkaTemplate<String, Object> kafkaTemplate,
                                KafkaTopicProperties topicProperties,
                                ObjectMapper objectMapper) {
        this.studentLinkRepository = studentLinkRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "${kafka.topics.assess-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        properties = {
            "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"
        }
    )
    public void handleAssessEvent(String eventJson) {
        try {
            JsonNode node = objectMapper.readTree(eventJson);
            // Detect GradeIssuedEvent by presence of studentId + percentage + passed
            if (node.has("studentId") && node.has("percentage") && node.has("passed")) {
                handleGradeIssued(node);
            }
            // ExamPublishedEvent detection: has title + batchId but no studentId
            // Parent notification for exam announcements is handled by assess-svc directly to enrolled students.
            // Parent awareness of new exams can be a future feature once batch-parent mapping is available.
        } catch (Exception ex) {
            log.error("Failed to process assess event: {}", ex.getMessage(), ex);
        }
    }

    private void handleGradeIssued(JsonNode node) {
        UUID studentId = UUID.fromString(node.get("studentId").asText());
        UUID examId    = UUID.fromString(node.get("examId").asText());
        double pct     = node.get("percentage").asDouble();
        boolean passed = node.get("passed").asBoolean();

        // Look up all active parent links for this student
        List<StudentLink> links = studentLinkRepository.findActiveByStudentId(studentId);
        if (links.isEmpty()) {
            log.debug("No active parent links for studentId={} — skipping parent notification", studentId);
            return;
        }

        String resultStatus = passed ? "Passed ✓" : "Did not pass";
        for (StudentLink link : links) {
            // link.getParentId() is the parent PROFILE ID (see frozen fix)
            parentProfileRepository.findById(link.getParentId()).ifPresent(profile -> {
                // profile.getUserId() is the JWT user ID needed for notification delivery
                notifyParent(profile, examId, pct, resultStatus);
            });
        }
    }

    private void notifyParent(ParentProfile profile, UUID examId, double pct, String resultStatus) {
        String body = "Your child's exam result is ready. "
                + "Score: " + String.format("%.1f", pct) + "% — " + resultStatus + ". "
                + "View the full breakdown in Performance on the Parent Dashboard.";

        NotificationSendEvent event = NotificationSendEvent.inApp(
                profile.getUserId(),
                "Child's Result Published",
                body,
                Map.of("notificationType", "RESULT_PUBLISHED",
                       "actionUrl",        "/parent",
                       "examId",           examId.toString())
        );

        kafkaTemplate.send(topicProperties.notificationSend(), profile.getUserId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish parent result notification: parentUserId={} error={}",
                            profile.getUserId(), ex.getMessage());
                } else {
                    log.debug("Parent result notification published: parentUserId={}", profile.getUserId());
                }
            });
    }
}
