// src/main/java/com/edutech/center/application/dto/AssignFeeRequest.java
package com.edutech.center.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AssignFeeRequest(
    @NotNull UUID feeStructureId,
    @NotNull LocalDate effectiveFrom,
    LocalDate effectiveTo
) {}
