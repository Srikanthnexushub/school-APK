package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.RoleContext;
import java.util.UUID;

public record ChatSessionStartResult(
    UUID sessionId,
    String greeting,
    RoleContext context
) {}
