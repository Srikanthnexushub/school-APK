package com.edutech.events.center;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published by center-svc to the center-events topic when an announcement is delivered.
 * Consumed by parent-svc to fan out in-app notifications to affected parents.
 *
 * <p>Topic: {@code KAFKA_TOPIC_CENTER_EVENTS}</p>
 */
public record AnnouncementCreatedEvent(
        UUID eventId,
        UUID announcementId,
        UUID centerId,
        String title,
        String body,
        String targetType,        // BATCH | CENTER | ROLE | ALL
        UUID targetBatchId,       // non-null only when targetType=BATCH
        List<UUID> batchStudentIds, // non-null only when targetType=BATCH
        String targetRole,        // STUDENT | TEACHER | PARENT | ALL | null
        Instant occurredAt
) {
    public AnnouncementCreatedEvent(UUID announcementId, UUID centerId, String title, String body,
                                    String targetType, UUID targetBatchId, List<UUID> batchStudentIds,
                                    String targetRole) {
        this(UUID.randomUUID(), announcementId, centerId, title, body,
             targetType, targetBatchId, batchStudentIds, targetRole, Instant.now());
    }
}
