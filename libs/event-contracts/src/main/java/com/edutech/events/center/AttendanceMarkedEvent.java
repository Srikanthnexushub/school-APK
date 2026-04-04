package com.edutech.events.center;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Published by center-svc to the center-events topic when attendance is marked for a batch.
 * Consumed by parent-svc to fan out absent/late notifications to parents.
 *
 * <p>Topic: {@code KAFKA_TOPIC_CENTER_EVENTS}</p>
 */
public record AttendanceMarkedEvent(
        UUID eventId,
        UUID batchId,
        UUID centerId,
        String batchName,
        LocalDate date,
        UUID markedByUserId,
        List<StudentAttendanceRecord> records,
        Instant occurredAt
) {
    public record StudentAttendanceRecord(UUID studentId, String studentName, String status) {}

    /** Convenience constructor — generates eventId and occurredAt automatically. */
    public AttendanceMarkedEvent(UUID batchId, UUID centerId, String batchName,
                                  LocalDate date, UUID markedByUserId,
                                  List<StudentAttendanceRecord> records) {
        this(UUID.randomUUID(), batchId, centerId, batchName, date, markedByUserId,
             List.copyOf(records), Instant.now());
    }
}
