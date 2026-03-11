// src/main/java/com/edutech/parent/application/dto/RecordFeePaymentRequest.java
package com.edutech.parent.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecordFeePaymentRequest(
        @NotNull UUID studentId,
        UUID centerId,
        UUID batchId,
        @NotNull @DecimalMin("0.01") BigDecimal amountPaid,
        String currency,
        @NotNull LocalDate paymentDate,
        @NotBlank String referenceNumber,
        String remarks,
        String feeType,
        String paymentMethod
) {}
