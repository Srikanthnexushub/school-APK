// src/main/java/com/edutech/parent/infrastructure/messaging/CenterEventConsumer.java
package com.edutech.parent.infrastructure.messaging;

import com.edutech.events.center.AnnouncementCreatedEvent;
import com.edutech.parent.domain.model.StudentLink;
import com.edutech.parent.domain.port.out.NotificationPublisher;
import com.edutech.parent.domain.port.out.ParentProfileRepository;
import com.edutech.parent.domain.port.out.StudentLinkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Consumes domain events from center-svc.
 * Handles AnnouncementCreatedEvent to fan out in-app notifications to affected parents.
 */
@Component
public class CenterEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CenterEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final StudentLinkRepository studentLinkRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final NotificationPublisher notificationPublisher;

    public CenterEventConsumer(ObjectMapper objectMapper,
                               StudentLinkRepository studentLinkRepository,
                               ParentProfileRepository parentProfileRepository,
                               NotificationPublisher notificationPublisher) {
        this.objectMapper = objectMapper;
        this.studentLinkRepository = studentLinkRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.notificationPublisher = notificationPublisher;
    }

    @KafkaListener(
        topics = "${kafka.topics.center-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        properties = {
            "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"
        }
    )
    public void handleCenterEvent(String eventJson) {
        try {
            // Detect event type by checking for announcementId field
            if (eventJson.contains("\"announcementId\"")) {
                AnnouncementCreatedEvent event = objectMapper.readValue(eventJson, AnnouncementCreatedEvent.class);
                handleAnnouncementCreated(event);
            } else {
                log.debug("Received unhandled center-svc event: {}", eventJson);
            }
        } catch (Exception e) {
            log.warn("Failed to process center-svc event: {} — error: {}", eventJson, e.getMessage());
        }
    }

    private void handleAnnouncementCreated(AnnouncementCreatedEvent event) {
        String targetType = event.targetType();
        String targetRole = event.targetRole();

        // Determine if parents should receive this announcement
        boolean includeParents = "CENTER".equals(targetType) || "ALL".equals(targetType)
                || "PARENT".equals(targetRole) || "ALL".equals(targetRole)
                || ("BATCH".equals(targetType) && event.targetBatchId() != null)
                || (targetRole == null && targetType != null);

        if (!includeParents) {
            log.debug("Announcement {} targetType={} targetRole={} — skipping parent fanout",
                    event.announcementId(), targetType, targetRole);
            return;
        }

        Set<UUID> parentUserIds = new LinkedHashSet<>();

        if ("BATCH".equals(targetType) && event.batchStudentIds() != null && !event.batchStudentIds().isEmpty()) {
            // BATCH target: find parents of students in that specific batch
            for (UUID studentId : event.batchStudentIds()) {
                studentLinkRepository.findActiveByStudentId(studentId).stream()
                        .map(StudentLink::getParentId)
                        .forEach(parentProfileId ->
                            parentProfileRepository.findById(parentProfileId)
                                .ifPresent(profile -> parentUserIds.add(profile.getUserId())));
            }
        } else {
            // CENTER / ROLE / ALL: find all parents linked to this center
            studentLinkRepository.findActiveByCenterId(event.centerId()).stream()
                    .map(StudentLink::getParentId)
                    .forEach(parentProfileId ->
                        parentProfileRepository.findById(parentProfileId)
                            .ifPresent(profile -> parentUserIds.add(profile.getUserId())));
        }

        Map<String, String> metadata = Map.of(
                "announcementId", event.announcementId().toString(),
                "centerId", event.centerId().toString(),
                "targetType", targetType != null ? targetType : "UNKNOWN"
        );

        for (UUID parentUserId : parentUserIds) {
            notificationPublisher.sendInApp(parentUserId, event.title(), event.body(), metadata);
        }

        log.info("Announcement {} parent fanout: centerId={} parentCount={}",
                event.announcementId(), event.centerId(), parentUserIds.size());
    }
}
