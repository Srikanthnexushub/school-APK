// src/main/java/com/edutech/center/domain/event/ScheduleChangedEvent.java
package com.edutech.center.domain.event;

import java.time.Instant;
import java.util.UUID;

public record ScheduleChangedEvent(
    UUID eventId,
    UUID scheduleId,
    UUID batchId,
    UUID centerId,
    String changeType,
    Instant occurredAt
) {
    public ScheduleChangedEvent(UUID scheduleId, UUID batchId, UUID centerId, String changeType) {
        this(UUID.randomUUID(), scheduleId, batchId, centerId, changeType, Instant.now());
    }
}
