package com.edutech.aigateway.application.dto;

import com.edutech.aigateway.domain.model.Role;

import java.util.UUID;

public record AuthPrincipal(UUID userId, String email, Role role, UUID centerId, String deviceFP) {

    public boolean isSuperAdmin() {
        return role == Role.SUPER_ADMIN;
    }
}
