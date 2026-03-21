package com.edutech.psych.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePsychProfileRequest(
        @NotNull UUID studentId,
        UUID centerId,   // optional — null for self-service (no center)
        UUID batchId     // optional — null for self-service
) {
}
