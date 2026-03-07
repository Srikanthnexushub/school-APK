package com.edutech.psych.application.dto;

import com.edutech.psych.domain.model.SessionType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record StartSessionRequest(
        @NotNull SessionType sessionType,
        @NotNull Instant scheduledAt
) {
}
