// src/main/java/com/edutech/center/application/dto/FeeStructureResponse.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.FeeFrequency;
import com.edutech.center.domain.model.FeeStructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FeeStructureResponse(
    UUID id,
    UUID centerId,
    String name,
    String description,
    BigDecimal amount,
    String currency,
    FeeFrequency frequency,
    int dueDay,
    BigDecimal lateFeeAmount,
    FeeStructure.FeeStatus status,
    Instant createdAt
) {}
