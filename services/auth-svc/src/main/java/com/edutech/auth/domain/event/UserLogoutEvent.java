// src/main/java/com/edutech/auth/domain/event/UserLogoutEvent.java
package com.edutech.auth.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain event — emitted on user logout (single session or all sessions).
 */
public record UserLogoutEvent(
    UUID eventId,
    UUID userId,
    String tokenId,
    boolean allSessions,
    Instant occurredAt
) {
    public UserLogoutEvent(UUID userId, String tokenId, boolean allSessions) {
        this(UUID.randomUUID(), userId, tokenId, allSessions, Instant.now());
    }
}
