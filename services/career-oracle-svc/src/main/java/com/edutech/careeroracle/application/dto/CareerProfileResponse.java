package com.edutech.careeroracle.application.dto;

import com.edutech.careeroracle.domain.model.CareerStream;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CareerProfileResponse(
        UUID id,
        UUID studentId,
        UUID enrollmentId,
        String academicStream,
        Integer currentGrade,
        BigDecimal ersScore,
        CareerStream preferredCareerStream,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
