// src/main/java/com/edutech/parent/application/dto/AuthPrincipal.java
package com.edutech.parent.application.dto;

import com.edutech.parent.domain.model.Role;

import java.util.UUID;

public record AuthPrincipal(
        UUID userId,
        String email,
        Role role,
        UUID centerId,
        String deviceFingerprintHash
) {
    public boolean isSuperAdmin() {
        return role == Role.SUPER_ADMIN;
    }

    public boolean isParent() {
        return role == Role.PARENT;
    }

    public boolean ownsProfile(UUID profileUserId) {
        return isSuperAdmin() || userId.equals(profileUserId);
    }
}
