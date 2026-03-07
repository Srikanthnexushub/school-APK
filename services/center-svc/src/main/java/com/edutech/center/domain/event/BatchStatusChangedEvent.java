// src/main/java/com/edutech/center/domain/event/BatchStatusChangedEvent.java
package com.edutech.center.domain.event;

import com.edutech.center.domain.model.BatchStatus;

import java.time.Instant;
import java.util.UUID;

public record BatchStatusChangedEvent(
    UUID eventId,
    UUID batchId,
    UUID centerId,
    BatchStatus previousStatus,
    BatchStatus newStatus,
    Instant occurredAt
) {
    public BatchStatusChangedEvent(UUID batchId, UUID centerId,
                                   BatchStatus previousStatus, BatchStatus newStatus) {
        this(UUID.randomUUID(), batchId, centerId, previousStatus, newStatus, Instant.now());
    }
}
