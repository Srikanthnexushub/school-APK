// src/main/java/com/edutech/assess/application/dto/AuthPrincipal.java
package com.edutech.assess.application.dto;

import com.edutech.assess.domain.model.Role;

import java.util.UUID;

public record AuthPrincipal(
        UUID userId,
        String email,
        Role role,
        UUID centerId,
        String deviceFingerprintHash
) {
    public boolean isSuperAdmin() { return role == Role.SUPER_ADMIN; }
    public boolean isInstitutionAdmin() { return role == Role.INSTITUTION_ADMIN; }
    public boolean isCenterAdmin() { return role == Role.CENTER_ADMIN; }
    public boolean isTeacher() { return role == Role.TEACHER; }
    public boolean isStudent() { return role == Role.STUDENT; }

    public boolean belongsToCenter(UUID cid) {
        return isSuperAdmin() || isInstitutionAdmin() || (cid != null && cid.equals(this.centerId));
    }
}
