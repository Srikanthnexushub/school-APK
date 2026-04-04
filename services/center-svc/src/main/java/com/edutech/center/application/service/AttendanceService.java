// src/main/java/com/edutech/center/application/service/AttendanceService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AttendanceEntry;
import com.edutech.center.application.dto.AttendanceResponse;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.MarkAttendanceRequest;
import com.edutech.center.application.exception.BatchNotFoundException;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.domain.model.Attendance;
import com.edutech.center.domain.model.AttendanceStatus;
import com.edutech.center.domain.model.Batch;
import com.edutech.center.domain.port.in.MarkAttendanceUseCase;
import com.edutech.center.domain.port.out.AttendanceRepository;
import com.edutech.center.domain.port.out.BatchRepository;
import com.edutech.center.domain.port.out.TeacherRepository;
import com.edutech.center.infrastructure.config.KafkaTopicProperties;
import com.edutech.events.center.AttendanceMarkedEvent;
import com.edutech.events.notification.NotificationSendEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AttendanceService implements MarkAttendanceUseCase {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceRepository attendanceRepository;
    private final BatchRepository batchRepository;
    private final TeacherRepository teacherRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             BatchRepository batchRepository,
                             TeacherRepository teacherRepository,
                             KafkaTemplate<String, Object> kafkaTemplate,
                             KafkaTopicProperties topicProperties) {
        this.attendanceRepository = attendanceRepository;
        this.batchRepository = batchRepository;
        this.teacherRepository = teacherRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    @Transactional
    public List<AttendanceResponse> markAttendance(UUID batchId, MarkAttendanceRequest request,
                                                   AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new BatchNotFoundException(batchId));

        if (!principal.belongsToCenter(batch.getCenterId())) {
            // TEACHER: JWT centerId is null post-approval; verify via DB lookup
            if (principal.isTeacher()) {
                boolean belongs = teacherRepository.findByUserId(principal.userId())
                        .stream().anyMatch(t -> t.getCenterId().equals(batch.getCenterId()));
                if (!belongs) throw new CenterAccessDeniedException();
            } else {
                throw new CenterAccessDeniedException();
            }
        }

        // Re-marking replaces existing records for the date
        attendanceRepository.deleteByBatchIdAndDate(batchId, request.date());

        List<Attendance> records = request.entries().stream()
            .map(entry -> Attendance.mark(batchId, batch.getCenterId(),
                    entry.studentId(), request.date(),
                    entry.status(), principal.userId(), entry.notes()))
            .toList();

        List<Attendance> saved = attendanceRepository.saveAll(records);

        log.info("Attendance marked: batchId={} date={} count={}",
                batchId, request.date(), saved.size());

        // Best-effort notifications — do NOT let failures roll back the main transaction
        try {
            publishNotifications(batch, saved, request.entries(), request.date(), principal.userId());
        } catch (Exception e) {
            log.error("Attendance notification publish failed — attendance was saved: batchId={} error={}",
                    batchId, e.getMessage());
        }

        return saved.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendance(UUID batchId, LocalDate date, AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new BatchNotFoundException(batchId));
        if (!principal.belongsToCenter(batch.getCenterId())) {
            // TEACHER: JWT centerId is null post-approval; verify via DB lookup
            if (principal.isTeacher()) {
                boolean belongs = teacherRepository.findByUserId(principal.userId())
                        .stream().anyMatch(t -> t.getCenterId().equals(batch.getCenterId()));
                if (!belongs) throw new CenterAccessDeniedException();
            } else {
                throw new CenterAccessDeniedException();
            }
        }
        return attendanceRepository.findByBatchIdAndDate(batchId, date)
                .stream().map(this::toResponse).toList();
    }

    // ─── Notifications ────────────────────────────────────────────────────────

    /**
     * Publishes two Kafka events after attendance is saved:
     * 1. IN_APP notification directly to each student (all statuses for awareness,
     *    absent/late highlighted with status emoji).
     * 2. AttendanceMarkedEvent on center-events for parent-svc to fan out to parents.
     *
     * Both are best-effort — failures are logged and do NOT affect the response.
     */
    private void publishNotifications(Batch batch, List<Attendance> saved,
                                       List<AttendanceEntry> entries,
                                       LocalDate date, UUID markedByUserId) {
        // 1. Student IN_APP notifications
        for (Attendance a : saved) {
            String statusLabel = buildStatusLabel(a.getStatus());
            String subject = "Attendance marked \u2014 " + batch.getName();
            String body = "Your attendance for " + date + " (" + batch.getName() + ")"
                        + " has been recorded as: " + statusLabel;
            Map<String, String> meta = Map.of(
                    "batchId", batch.getId().toString(),
                    "date", date.toString(),
                    "status", a.getStatus().name(),
                    "notificationType", "ATTENDANCE_MARKED",
                    "actionUrl", "/attendance"
            );
            NotificationSendEvent studentEvent = NotificationSendEvent.inApp(a.getStudentId(), subject, body, meta);
            kafkaTemplate.send(topicProperties.notificationSend(), studentEvent);
        }

        // 2. Parent fan-out via center-events → parent-svc
        List<AttendanceMarkedEvent.StudentAttendanceRecord> studentRecords = saved.stream()
                .map(a -> new AttendanceMarkedEvent.StudentAttendanceRecord(
                        a.getStudentId(), "", a.getStatus().name()))
                .collect(Collectors.toList());

        AttendanceMarkedEvent attendanceEvent = new AttendanceMarkedEvent(
                batch.getId(), batch.getCenterId(), batch.getName(),
                date, markedByUserId, studentRecords);
        kafkaTemplate.send(topicProperties.centerEvents(), attendanceEvent);

        log.debug("Attendance notifications published: batchId={} date={} studentCount={}",
                batch.getId(), date, saved.size());
    }

    private String buildStatusLabel(AttendanceStatus status) {
        return switch (status) {
            case PRESENT -> "Present";
            case ABSENT  -> "Absent";
            case LATE    -> "Late";
            case EXCUSED -> "Excused";
        };
    }

    private AttendanceResponse toResponse(Attendance a) {
        return new AttendanceResponse(a.getId(), a.getBatchId(), a.getStudentId(),
                a.getDate(), a.getStatus(), a.getMarkedByTeacherId(),
                a.getNotes(), a.getCreatedAt());
    }
}
