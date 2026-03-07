// src/main/java/com/edutech/center/domain/event/TeacherAssignedEvent.java
package com.edutech.center.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TeacherAssignedEvent(
    UUID eventId,
    UUID teacherId,
    UUID centerId,
    UUID userId,
    String email,
    Instant occurredAt
) {
    public TeacherAssignedEvent(UUID teacherId, UUID centerId, UUID userId, String email) {
        this(UUID.randomUUID(), teacherId, centerId, userId, email, Instant.now());
    }
}
