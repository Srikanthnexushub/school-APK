package com.edutech.chat.domain.model;

import java.util.List;
import java.util.UUID;

public record ParentContext(
    UUID userId,
    String fullName,
    List<String> linkedStudentNames,
    String currentPage
) implements RoleContext {

    @Override
    public String role() { return "PARENT"; }

    public static ParentContext empty(UUID userId, String currentPage) {
        return new ParentContext(userId, "Parent", List.of(), currentPage);
    }
}
