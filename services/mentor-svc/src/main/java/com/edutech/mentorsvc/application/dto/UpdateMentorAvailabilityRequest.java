package com.edutech.mentorsvc.application.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMentorAvailabilityRequest(
        @NotNull(message = "available flag is required")
        Boolean available
) {
}
