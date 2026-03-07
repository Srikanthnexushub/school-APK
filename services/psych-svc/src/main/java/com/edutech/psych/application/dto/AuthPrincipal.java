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

    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    public boolean belongsToCenter(UUID cId) {
        return isSuperAdmin() || (centerId != null && centerId.equals(cId));
    }
}
