package com.edutech.mentorsvc.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitFeedbackRequest(
        @NotNull(message = "studentId is required")
        UUID studentId,

        @Min(value = 1, message = "rating must be at least 1")
        @Max(value = 5, message = "rating must be at most 5")
        int rating,

        String comment
) {
}
