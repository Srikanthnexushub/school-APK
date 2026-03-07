// src/main/java/com/edutech/parent/domain/event/FeePaymentRecordedEvent.java
package com.edutech.parent.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FeePaymentRecordedEvent(
        UUID eventId,
        UUID paymentId,
        UUID parentId,
        UUID studentId,
        UUID centerId,
        UUID batchId,
        BigDecimal amountPaid,
        String currency,
        Instant occurredAt
) {
    public FeePaymentRecordedEvent(UUID paymentId, UUID parentId, UUID studentId,
                                    UUID centerId, UUID batchId, BigDecimal amountPaid, String currency) {
        this(UUID.randomUUID(), paymentId, parentId, studentId, centerId, batchId, amountPaid, currency, Instant.now());
    }
}
