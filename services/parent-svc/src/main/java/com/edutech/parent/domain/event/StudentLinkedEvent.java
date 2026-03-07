// src/main/java/com/edutech/parent/domain/event/StudentLinkedEvent.java
package com.edutech.parent.domain.event;

import java.time.Instant;
import java.util.UUID;

public record StudentLinkedEvent(
        UUID eventId,
        UUID linkId,
        UUID parentId,
        UUID studentId,
        UUID centerId,
        Instant occurredAt
) {
    public StudentLinkedEvent(UUID linkId, UUID parentId, UUID studentId, UUID centerId) {
        this(UUID.randomUUID(), linkId, parentId, studentId, centerId, Instant.now());
    }
}
