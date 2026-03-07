package com.edutech.performance.application.dto;

import com.edutech.performance.domain.model.ErrorType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WeakAreaResponse(
        UUID id,
        UUID studentId,
        String subject,
        String topicName,
        BigDecimal masteryPercent,
        ErrorType primaryErrorType,
        Integer incorrectAttempts,
        Integer totalAttempts,
        Boolean prerequisitesWeak,
        Instant detectedAt
) {
}
