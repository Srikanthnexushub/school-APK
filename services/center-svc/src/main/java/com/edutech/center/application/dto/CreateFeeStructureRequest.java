// src/main/java/com/edutech/center/application/dto/CreateFeeStructureRequest.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.FeeFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateFeeStructureRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 1000) String description,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank @Size(max = 5) String currency,
    @NotNull FeeFrequency frequency,
    @Min(1) @Max(31) int dueDay,
    @DecimalMin("0.00") BigDecimal lateFeeAmount
) {}
