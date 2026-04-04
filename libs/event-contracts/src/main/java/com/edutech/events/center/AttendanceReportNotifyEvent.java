package com.edutech.events.center;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Published by center-svc when admin/teacher triggers "Send Report to Parents".
 * Consumed by parent-svc to fan out attendance summary notifications to all parents
 * of students in the batch.
 *
 * <p>Topic: {@code KAFKA_TOPIC_CENTER_EVENTS}</p>
 */
public record AttendanceReportNotifyEvent(
        UUID eventId,
        UUID batchId,
        UUID centerId,
        String batchName,
        LocalDate from,
        LocalDate to,
        int totalStudents,
        int atRiskCount,
        double batchAveragePercent,
        List<UUID> batchStudentIds,
        Instant occurredAt
) {
    /** Convenience constructor — generates eventId and occurredAt automatically. */
    public AttendanceReportNotifyEvent(UUID batchId, UUID centerId, String batchName,
                                       LocalDate from, LocalDate to,
                                       int totalStudents, int atRiskCount,
                                       double batchAveragePercent, List<UUID> batchStudentIds) {
        this(UUID.randomUUID(), batchId, centerId, batchName, from, to,
             totalStudents, atRiskCount, batchAveragePercent,
             List.copyOf(batchStudentIds), Instant.now());
    }
}
