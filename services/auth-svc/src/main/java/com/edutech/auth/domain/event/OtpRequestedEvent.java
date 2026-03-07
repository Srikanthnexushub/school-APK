// src/main/java/com/edutech/auth/domain/event/OtpRequestedEvent.java
package com.edutech.auth.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain event — emitted when an OTP is generated.
 * Triggers the notification pipeline (email/SMS delivery).
 */
public record OtpRequestedEvent(
    UUID eventId,
    String email,
    String purpose,
    String channel,
    Instant occurredAt
) {
    public OtpRequestedEvent(String email, String purpose, String channel) {
        this(UUID.randomUUID(), email, purpose, channel, Instant.now());
    }
}
