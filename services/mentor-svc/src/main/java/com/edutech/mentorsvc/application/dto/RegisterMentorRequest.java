package com.edutech.mentorsvc.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterMentorRequest(
        @NotNull(message = "userId is required")
        UUID userId,

        @NotBlank(message = "fullName is required")
        String fullName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        String bio,

        String specializations,

        @Min(value = 0, message = "yearsOfExperience must be 0 or greater")
        int yearsOfExperience,

        @DecimalMin(value = "0.0", inclusive = false, message = "hourlyRate must be positive")
        BigDecimal hourlyRate
) {
}
