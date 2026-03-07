package com.edutech.psych.application.dto;

import com.edutech.psych.domain.model.CareerMappingStatus;

import java.time.Instant;
import java.util.UUID;

public record CareerMappingResponse(
        UUID id,
        UUID profileId,
        UUID studentId,
        CareerMappingStatus status,
        Instant requestedAt,
        Instant generatedAt,
        String topCareers,
        String reasoning,
        String modelVersion,
        Instant createdAt
) {
}
