package com.edutech.psych.application.dto;

import com.edutech.psych.domain.model.Role;

import java.util.UUID;

public record AuthPrincipal(
        UUID userId,
        String email,
        Role role,
        UUID centerId,
        String deviceFP
) {
    public boolean isSuperAdmin() {
        return role == Role.SUPER_ADMIN;
    }

    public boolean isInstitutionAdmin() {
        return role == Role.INSTITUTION_ADMIN;
    }

    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    public boolean isParent() {
        return role == Role.PARENT;
    }

    public boolean belongsToCenter(UUID cId) {
        return isSuperAdmin() || isInstitutionAdmin() || (centerId != null && centerId.equals(cId));
    }
}
