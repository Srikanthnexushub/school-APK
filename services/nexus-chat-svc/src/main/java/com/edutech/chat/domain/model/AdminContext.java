package com.edutech.chat.domain.model;

import java.util.UUID;

public record AdminContext(
    UUID userId,
    String fullName,
    String centerName,
    UUID centerId,
    String currentPage
) implements RoleContext {

    @Override
    public String role() { return "CENTER_ADMIN"; }

    public static AdminContext empty(UUID userId, UUID centerId, String currentPage) {
        return new AdminContext(userId, "Admin", null, centerId, currentPage);
    }
}
