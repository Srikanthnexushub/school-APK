package com.edutech.chat.domain.model;

import java.util.List;
import java.util.UUID;

public record TeacherContext(
    UUID userId,
    String fullName,
    String bio,
    List<String> specializations,
    String centerName,
    UUID centerId,
    String currentPage
) implements RoleContext {

    @Override
    public String role() { return "TEACHER"; }

    public static TeacherContext empty(UUID userId, UUID centerId, String currentPage) {
        return new TeacherContext(userId, "Teacher", null, List.of(), null, centerId, currentPage);
    }
}
