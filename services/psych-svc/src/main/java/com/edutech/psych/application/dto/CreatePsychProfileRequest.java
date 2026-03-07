package com.edutech.psych.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePsychProfileRequest(
        @NotNull UUID studentId,
        @NotNull UUID centerId,
        @NotNull UUID batchId
) {
}
