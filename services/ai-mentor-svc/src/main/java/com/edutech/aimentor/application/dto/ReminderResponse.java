package com.edutech.aimentor.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ReminderResponse(
        UUID id,
        UUID studyPlanId,
        UUID studentId,
        String reminderType,
        Instant remindAt,
        String title,
        String message,
        boolean acknowledged,
        Instant acknowledgedAt,
        Instant createdAt
) {}
