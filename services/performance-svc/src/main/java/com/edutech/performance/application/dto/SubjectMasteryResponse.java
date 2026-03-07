package com.edutech.performance.application.dto;

import com.edutech.performance.domain.model.MasteryLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubjectMasteryResponse(
        UUID id,
        UUID studentId,
        String subject,
        BigDecimal masteryPercent,
        MasteryLevel masteryLevel,
        BigDecimal velocityPerWeek,
        Integer totalTopics,
        Integer masteredTopics,
        Instant lastUpdatedAt
) {
}
