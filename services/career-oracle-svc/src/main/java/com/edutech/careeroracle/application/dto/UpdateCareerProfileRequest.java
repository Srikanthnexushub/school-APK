package com.edutech.careeroracle.application.dto;

import com.edutech.careeroracle.domain.model.CareerStream;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateCareerProfileRequest(
        @NotBlank String academicStream,
        @NotNull @Min(1) @Max(12) Integer currentGrade,
        @Min(0) @Max(100) BigDecimal ersScore,
        CareerStream preferredCareerStream
) {
}
