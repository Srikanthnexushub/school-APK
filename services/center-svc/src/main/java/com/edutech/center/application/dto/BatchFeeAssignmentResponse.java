// src/main/java/com/edutech/center/application/dto/BatchFeeAssignmentResponse.java
package com.edutech.center.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BatchFeeAssignmentResponse(
    UUID id,
    UUID batchId,
    UUID centerId,
    UUID feeStructureId,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    Instant createdAt
) {}
