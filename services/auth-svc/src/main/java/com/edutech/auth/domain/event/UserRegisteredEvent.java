// src/main/java/com/edutech/auth/domain/event/UserRegisteredEvent.java
package com.edutech.auth.domain.event;

import com.edutech.auth.domain.model.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain event — emitted once when a new user account is created.
 * Published to Kafka audit topic; never updated or deleted after publication.
 */
public record UserRegisteredEvent(
    UUID eventId,
    UUID userId,
    String email,
    Role role,
    UUID centerId,
    Instant occurredAt
) {
    public UserRegisteredEvent(UUID userId, String email, Role role, UUID centerId) {
        this(UUID.randomUUID(), userId, email, role, centerId, Instant.now());
    }
}
