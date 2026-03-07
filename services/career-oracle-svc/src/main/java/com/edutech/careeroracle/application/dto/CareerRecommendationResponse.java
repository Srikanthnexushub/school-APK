package com.edutech.careeroracle.application.dto;

import com.edutech.careeroracle.domain.model.CareerStream;
import com.edutech.careeroracle.domain.model.ConfidenceLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CareerRecommendationResponse(
        UUID id,
        UUID studentId,
        CareerStream careerStream,
        BigDecimal fitScore,
        ConfidenceLevel confidenceLevel,
        String rationale,
        Integer rankOrder,
        OffsetDateTime generatedAt,
        OffsetDateTime validUntil,
        Boolean isActive
) {
}
