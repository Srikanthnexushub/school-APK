// src/main/java/com/edutech/auth/domain/event/TokenRefreshedEvent.java
package com.edutech.auth.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain event — emitted on every refresh token rotation.
 * Enables detection of refresh token reuse attacks.
 */
public record TokenRefreshedEvent(
    UUID eventId,
    UUID userId,
    String oldTokenId,
    String newTokenId,
    Instant occurredAt
) {
    public TokenRefreshedEvent(UUID userId, String oldTokenId, String newTokenId) {
        this(UUID.randomUUID(), userId, oldTokenId, newTokenId, Instant.now());
    }
}
