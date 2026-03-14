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
    public boolean isStudent() { return role == Role.STUDENT; }

    public boolean belongsToCenter(UUID targetCenterId) {
        return isSuperAdmin() || (centerId != null && centerId.equals(targetCenterId));
    }

    /**
     * Extended check that also grants access when the requesting user is the center's
     * designated admin (adminUserId).  This covers the case where a CENTER_ADMIN's JWT
     * still carries centerId=null because the Kafka-based sync from center-svc → auth-svc
     * has not fired yet (e.g. Kafka unavailable in local dev).
     */
    public boolean belongsToCenter(UUID targetCenterId, UUID centerAdminUserId) {
        return belongsToCenter(targetCenterId)
                || (isCenterAdmin() && userId != null && userId.equals(centerAdminUserId));
    }
}
