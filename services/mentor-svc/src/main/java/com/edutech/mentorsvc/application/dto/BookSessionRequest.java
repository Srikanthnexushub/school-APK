package com.edutech.mentorsvc.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookSessionRequest(
        @NotNull(message = "mentorId is required")
        UUID mentorId,

        @NotNull(message = "studentId is required")
        UUID studentId,

        @NotNull(message = "scheduledAt is required")
        OffsetDateTime scheduledAt,

        @Min(value = 15, message = "durationMinutes must be at least 15")
        int durationMinutes,

        @NotNull(message = "sessionMode is required")
        String sessionMode,

        String meetingLink,

        String notes
) {
}
