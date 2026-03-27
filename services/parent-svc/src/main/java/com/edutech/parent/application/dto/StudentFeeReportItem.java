// src/main/java/com/edutech/parent/application/dto/StudentFeeReportItem.java
package com.edutech.parent.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Billing report item for a single student in a center.
 *
 * <p>paymentStatus values: {@code FULLY_PAID}, {@code PARTIAL}, {@code NO_PAYMENT}</p>
 */
public record StudentFeeReportItem(
        UUID studentId,
        String studentName,
        BigDecimal totalPaid,
        int paymentCount,
        String paymentStatus
) {}
