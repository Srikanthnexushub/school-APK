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
 * Consumes assess-svc domain events to fan out notifications to parents.
 *
 * <p>On GradeIssuedEvent: looks up all active parents for the student and publishes
 * both an IN_APP notification (bell icon in portal) and an SMS to the parent's
 * registered phone number, so they are notified through two channels.</p>
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
        } catch (Exception ex) {
            log.error("Failed to process assess event: {}", ex.getMessage(), ex);
        }
    }

    private void handleGradeIssued(JsonNode node) {
        UUID studentId = UUID.fromString(node.get("studentId").asText());
        UUID examId    = UUID.fromString(node.get("examId").asText());
        double pct     = node.get("percentage").asDouble();
        boolean passed = node.get("passed").asBoolean();

        List<StudentLink> links = studentLinkRepository.findActiveByStudentId(studentId);
        if (links.isEmpty()) {
            log.debug("No active parent links for studentId={} — skipping parent notification", studentId);
            return;
        }

        String resultStatus = passed ? "Passed ✓" : "Did not pass";
        for (StudentLink link : links) {
            // link.getParentId() is the parent PROFILE ID (frozen fix — never change)
            parentProfileRepository.findById(link.getParentId()).ifPresent(profile -> {
                notifyParentInApp(profile, examId, pct, resultStatus);
                notifyParentSms(profile, pct, resultStatus);
            });
        }
    }

    private void notifyParentInApp(ParentProfile profile, UUID examId, double pct, String resultStatus) {
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
                    log.error("Failed to publish IN_APP parent notification: parentUserId={} error={}",
                            profile.getUserId(), ex.getMessage());
                } else {
                    log.debug("IN_APP parent notification published: parentUserId={}", profile.getUserId());
                }
            });
    }

    private void notifyParentSms(ParentProfile profile, double pct, String resultStatus) {
        String phone = profile.getPhone();
        if (phone == null || phone.isBlank()) {
            log.debug("No phone on parent profile id={} — skipping SMS", profile.getId());
            return;
        }

        String smsBody = "EduPath Alert: Your child's exam result is ready. "
                + "Score: " + String.format("%.1f", pct) + "% — " + resultStatus + ". "
                + "Login to your Parent Dashboard for the full breakdown.";

        NotificationSendEvent event = NotificationSendEvent.sms(
                profile.getUserId(),
                phone,
                "Child's Result Published",
                smsBody,
                Map.of("notificationType", "RESULT_PUBLISHED",
                       "actionUrl",        "/parent")
        );

        kafkaTemplate.send(topicProperties.notificationSend(), profile.getUserId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish SMS parent notification: parentUserId={} phone={} error={}",
                            profile.getUserId(), phone, ex.getMessage());
                } else {
                    log.debug("SMS parent notification published: parentUserId={} phone={}", profile.getUserId(), phone);
                }
            });
    }
}
