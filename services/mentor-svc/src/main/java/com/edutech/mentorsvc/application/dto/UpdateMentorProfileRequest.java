package com.edutech.mentorsvc.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateMentorProfileRequest(
        @Size(max = 255) String fullName,
        String bio,
        @Size(max = 500) String specializations,
        @Min(value = 0) Integer yearsOfExperience,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal hourlyRate,
        @Size(max = 20) String gender,
        @Size(max = 100) String district
) {
}
