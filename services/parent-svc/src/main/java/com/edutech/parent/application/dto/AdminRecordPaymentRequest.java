// src/main/java/com/edutech/parent/application/dto/AdminRecordPaymentRequest.java
package com.edutech.parent.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Admin-initiated fee payment request.
 * Does NOT require parentProfileId — the service resolves it via StudentLink.
 * Admin-recorded payments are auto-confirmed (status = CONFIRMED immediately).
 */
public record AdminRecordPaymentRequest(
        @NotNull UUID studentId,
        @NotNull UUID centerId,
        @NotNull @DecimalMin("0.01") BigDecimal amountPaid,
        @NotBlank String referenceNumber,
        @NotNull LocalDate paymentDate,
        String paymentMethod,   // defaults to CASH
        String feeType,         // defaults to TUITION
        String remarks
) {}
