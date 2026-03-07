// src/main/java/com/edutech/auth/domain/event/UserAuthenticatedEvent.java
package com.edutech.auth.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain event — emitted on every login attempt (success or failure).
 * Consumed by audit-svc and anomaly detection pipeline.
 */
public record UserAuthenticatedEvent(
    UUID eventId,
    UUID userId,
    String email,
    String ipAddress,
    String userAgent,
    boolean success,
    String failureReason,
    Instant occurredAt
) {
    /** Successful login */
    public UserAuthenticatedEvent(UUID userId, String email,
                                  String ipAddress, String userAgent) {
        this(UUID.randomUUID(), userId, email, ipAddress, userAgent,
             true, null, Instant.now());
    }

    /** Failed login attempt */
    public UserAuthenticatedEvent(String email, String ipAddress,
                                  String userAgent, String failureReason) {
        this(UUID.randomUUID(), null, email, ipAddress, userAgent,
             false, failureReason, Instant.now());
    }
}
