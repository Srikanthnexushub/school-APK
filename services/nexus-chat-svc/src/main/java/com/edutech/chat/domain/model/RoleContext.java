package com.edutech.chat.domain.model;

import java.util.UUID;

/**
 * Sealed interface implemented by every role-specific context record.
 * ContextAggregatorService returns a RoleContext; SystemPromptBuilder
 * and ChatSessionService pattern-match on the concrete type.
 */
public sealed interface RoleContext
        permits StudentContext, TeacherContext, AdminContext, ParentContext {

    UUID userId();
    String fullName();
    String role();
    String currentPage();
}
