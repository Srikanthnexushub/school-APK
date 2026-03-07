// src/main/java/com/edutech/center/domain/event/BatchCreatedEvent.java
package com.edutech.center.domain.event;

import java.time.Instant;
import java.util.UUID;

public record BatchCreatedEvent(
    UUID eventId,
    UUID batchId,
    UUID centerId,
    String batchName,
    String subject,
    UUID teacherId,
    Instant occurredAt
) {
    public BatchCreatedEvent(UUID batchId, UUID centerId, String batchName,
                             String subject, UUID teacherId) {
        this(UUID.randomUUID(), batchId, centerId, batchName, subject, teacherId, Instant.now());
    }
}
