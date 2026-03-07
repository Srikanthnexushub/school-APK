package com.edutech.psych.application.dto;

import com.edutech.psych.domain.model.ProfileStatus;

import java.time.Instant;
import java.util.UUID;

public record PsychProfileResponse(
        UUID id,
        UUID studentId,
        UUID centerId,
        UUID batchId,
        double openness,
        double conscientiousness,
        double extraversion,
        double agreeableness,
        double neuroticism,
        String riasecCode,
        ProfileStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
