// src/main/java/com/edutech/parent/domain/event/LinkRevokedEvent.java
package com.edutech.parent.domain.event;

import java.time.Instant;
import java.util.UUID;

public record LinkRevokedEvent(
        UUID eventId,
        UUID linkId,
        UUID parentId,
        UUID studentId,
        Instant occurredAt
) {
    public LinkRevokedEvent(UUID linkId, UUID parentId, UUID studentId) {
        this(UUID.randomUUID(), linkId, parentId, studentId, Instant.now());
    }
}
