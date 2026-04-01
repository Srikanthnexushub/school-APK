package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.StudentContext;
import java.util.UUID;

public record ChatSessionStartResult(
    UUID sessionId,
    String greeting,
    StudentContext context
) {}
