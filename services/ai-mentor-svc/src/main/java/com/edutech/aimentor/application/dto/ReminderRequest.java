package com.edutech.aimentor.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ReminderRequest(
        @NotNull UUID studyPlanId,
        @NotBlank String reminderType,
        @NotNull Instant remindAt,
        @NotBlank String title,
        String message
) {}
