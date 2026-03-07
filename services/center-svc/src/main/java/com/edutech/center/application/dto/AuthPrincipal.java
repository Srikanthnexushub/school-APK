// src/main/java/com/edutech/center/application/dto/AuthPrincipal.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.Role;

import java.util.UUID;

/**
 * The decoded identity extracted from a validated JWT.
 * Populated by JwtAuthenticationFilter and injected via @AuthenticationPrincipal.
 */
public record AuthPrincipal(
    UUID userId,
    String email,
    Role role,
    UUID centerId,
    String deviceFingerprintHash
) {
    public boolean isSuperAdmin() { return role == Role.SUPER_ADMIN; }
    public boolean isCenterAdmin() { return role == Role.CENTER_ADMIN; }
    public boolean isTeacher() { return role == Role.TEACHER; }

    public boolean belongsToCenter(UUID targetCenterId) {
        return isSuperAdmin() || (centerId != null && centerId.equals(targetCenterId));
    }
}
