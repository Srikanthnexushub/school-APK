package com.edutech.careeroracle.application.dto;

import com.edutech.careeroracle.domain.model.CollegeTier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CollegePredictionResponse(
        UUID id,
        UUID studentId,
        UUID enrollmentId,
        String collegeName,
        String courseName,
        CollegeTier collegeTier,
        BigDecimal predictedCutoff,
        BigDecimal studentPredictedScore,
        BigDecimal admissionProbability,
        OffsetDateTime generatedAt
) {
}
