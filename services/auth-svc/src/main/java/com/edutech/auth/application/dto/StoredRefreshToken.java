// src/main/java/com/edutech/auth/application/dto/StoredRefreshToken.java
package com.edutech.auth.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable value object stored in Redis for each active refresh token.
 */
public record StoredRefreshToken(
    UUID userId,
    String deviceFingerprintHash,
    Instant issuedAt,
    Instant expiresAt
) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isSameDevice(String fingerprintHash) {
        return this.deviceFingerprintHash.equals(fingerprintHash);
    }
}
