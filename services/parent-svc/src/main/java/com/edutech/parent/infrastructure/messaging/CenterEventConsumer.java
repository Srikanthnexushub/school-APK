// src/main/java/com/edutech/parent/infrastructure/messaging/CenterEventConsumer.java
package com.edutech.parent.infrastructure.messaging;

import com.edutech.events.center.AnnouncementCreatedEvent;
import com.edutech.events.center.AttendanceMarkedEvent;
import com.edutech.events.center.AttendanceReportNotifyEvent;
import com.edutech.parent.domain.model.StudentLink;
import com.edutech.parent.domain.port.out.NotificationPublisher;
import com.edutech.parent.domain.port.out.ParentProfileRepository;
import com.edutech.parent.domain.port.out.StudentLinkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Consumes domain events from center-svc.
 * <ul>
 *   <li>AnnouncementCreatedEvent → fan out in-app notifications to affected parents
 *   <li>AttendanceMarkedEvent   → notify parents of absent/late students
 * </ul>
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
            if (eventJson.contains("\"announcementId\"")) {
                AnnouncementCreatedEvent event = objectMapper.readValue(eventJson, AnnouncementCreatedEvent.class);
                handleAnnouncementCreated(event);
            } else if (eventJson.contains("\"markedByUserId\"")) {
                AttendanceMarkedEvent event = objectMapper.readValue(eventJson, AttendanceMarkedEvent.class);
                handleAttendanceMarked(event);
            } else if (eventJson.contains("\"batchAveragePercent\"")) {
                AttendanceReportNotifyEvent event = objectMapper.readValue(eventJson, AttendanceReportNotifyEvent.class);
                handleAttendanceReportNotify(event);
            } else {
                log.debug("Received unhandled center-svc event: {}", eventJson);
            }
        } catch (Exception e) {
            log.warn("Failed to process center-svc event: {} \u2014 error: {}", eventJson, e.getMessage());
        }
    }

    // ─── Announcement fan-out ─────────────────────────────────────────────────

    private void handleAnnouncementCreated(AnnouncementCreatedEvent event) {
        String targetType = event.targetType();
        String targetRole = event.targetRole();

        boolean includeParents = "CENTER".equals(targetType) || "ALL".equals(targetType)
                || "PARENT".equals(targetRole) || "ALL".equals(targetRole)
                || ("BATCH".equals(targetType) && event.targetBatchId() != null)
                || (targetRole == null && targetType != null);

        if (!includeParents) {
            log.debug("Announcement {} targetType={} targetRole={} \u2014 skipping parent fanout",
                    event.announcementId(), targetType, targetRole);
            return;
        }

        Set<UUID> parentUserIds = new LinkedHashSet<>();

        if ("BATCH".equals(targetType) && event.batchStudentIds() != null && !event.batchStudentIds().isEmpty()) {
            for (UUID studentId : event.batchStudentIds()) {
                resolveParentUserIds(studentId, parentUserIds);
            }
        } else {
            studentLinkRepository.findActiveByCenterId(event.centerId()).stream()
                    .map(StudentLink::getParentId)
                    .forEach(parentProfileId ->
                        parentProfileRepository.findById(parentProfileId)
                            .ifPresent(profile -> parentUserIds.add(profile.getUserId())));
        }

        Map<String, String> metadata = Map.of(
                "announcementId", event.announcementId().toString(),
                "centerId", event.centerId().toString(),
                "targetType", targetType != null ? targetType : "UNKNOWN",
                "notificationType", "ANNOUNCEMENT",
                "actionUrl", "/parent/announcements"
        );

        for (UUID parentUserId : parentUserIds) {
            notificationPublisher.sendInApp(parentUserId, event.title(), event.body(), metadata);
        }

        log.info("Announcement {} parent fanout: centerId={} parentCount={}",
                event.announcementId(), event.centerId(), parentUserIds.size());
    }

    // ─── Attendance parent notification ───────────────────────────────────────

    /**
     * For each absent or late student in the batch, resolves their parent(s) and sends
     * an IN_APP notification with the student name, batch name, date, and status.
     */
    private void handleAttendanceMarked(AttendanceMarkedEvent event) {
        int notified = 0;
        for (AttendanceMarkedEvent.StudentAttendanceRecord record : event.records()) {
            // Only notify parents for ABSENT or LATE — skip PRESENT/EXCUSED to avoid noise
            if (!"ABSENT".equals(record.status()) && !"LATE".equals(record.status())) continue;

            Set<UUID> parentUserIds = new LinkedHashSet<>();
            resolveParentUserIds(record.studentId(), parentUserIds);

            if (parentUserIds.isEmpty()) continue;

            String statusLabel = "ABSENT".equals(record.status()) ? "Absent" : "Late";
            String studentDisplay = record.studentName() == null || record.studentName().isBlank()
                    ? "Your child" : record.studentName();
            String subject = "Attendance Alert \u2014 " + event.batchName();
            String body = studentDisplay + " was marked " + statusLabel
                    + " for " + event.batchName() + " on " + event.date() + ".";

            Map<String, String> metadata = Map.of(
                    "batchId", event.batchId().toString(),
                    "centerId", event.centerId().toString(),
                    "studentId", record.studentId().toString(),
                    "date", event.date().toString(),
                    "status", record.status(),
                    "notificationType", "ATTENDANCE_MARKED",
                    "actionUrl", "/parent/attendance"
            );

            for (UUID parentUserId : parentUserIds) {
                notificationPublisher.sendInApp(parentUserId, subject, body, metadata);
                notified++;
            }
        }

        log.info("Attendance parent notifications sent: batchId={} date={} parentNotifications={}",
                event.batchId(), event.date(), notified);
    }

    // ─── Attendance report parent fanout ──────────────────────────────────────

    private void handleAttendanceReportNotify(AttendanceReportNotifyEvent event) {
        Set<UUID> parentUserIds = new LinkedHashSet<>();

        for (UUID studentId : event.batchStudentIds()) {
            resolveParentUserIds(studentId, parentUserIds);
        }

        if (parentUserIds.isEmpty()) {
            log.debug("AttendanceReportNotify: no parents found for batchId={}", event.batchId());
            return;
        }

        String subject = "Attendance Report \u2014 " + event.batchName();
        String body = String.format(
                "The attendance report for %s (%s to %s) is ready. " +
                "Batch average: %.1f%%. %d student(s) are below the 75%% threshold.",
                event.batchName(), event.from(), event.to(),
                event.batchAveragePercent(), event.atRiskCount()
        );

        Map<String, String> metadata = Map.of(
                "batchId", event.batchId().toString(),
                "centerId", event.centerId().toString(),
                "from", event.from().toString(),
                "to", event.to().toString(),
                "notificationType", "ATTENDANCE_REPORT",
                "actionUrl", "/parent/attendance"
        );

        for (UUID parentUserId : parentUserIds) {
            notificationPublisher.sendInApp(parentUserId, subject, body, metadata);
        }

        log.info("AttendanceReportNotify parent fanout: batchId={} parentCount={} atRisk={}",
                event.batchId(), parentUserIds.size(), event.atRiskCount());
    }

    // ─── Shared helper ────────────────────────────────────────────────────────

    private void resolveParentUserIds(UUID studentId, Set<UUID> parentUserIds) {
        studentLinkRepository.findActiveByStudentId(studentId).stream()
                .map(StudentLink::getParentId)
                .forEach(parentProfileId ->
                    parentProfileRepository.findById(parentProfileId)
                        .ifPresent(profile -> parentUserIds.add(profile.getUserId())));
    }
}
