// src/main/java/com/edutech/parent/application/dto/FeePaymentResponse.java
package com.edutech.parent.application.dto;

import com.edutech.parent.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FeePaymentResponse(
        UUID id,
        UUID parentId,
        UUID studentId,
        UUID centerId,
        UUID batchId,
        BigDecimal amountPaid,
        String currency,
        LocalDate paymentDate,
        String referenceNumber,
        String remarks,
        PaymentStatus status,
        Instant createdAt
) {}
