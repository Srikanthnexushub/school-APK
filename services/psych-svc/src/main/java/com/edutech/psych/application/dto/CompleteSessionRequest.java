package com.edutech.psych.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CompleteSessionRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") double openness,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") double conscientiousness,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") double extraversion,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") double agreeableness,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") double neuroticism,
        String riasecCode,
        String notes
) {
}
