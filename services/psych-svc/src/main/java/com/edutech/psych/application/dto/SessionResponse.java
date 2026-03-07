package com.edutech.psych.application.dto;

import com.edutech.psych.domain.model.SessionStatus;
import com.edutech.psych.domain.model.SessionType;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID profileId,
        UUID studentId,
        SessionType sessionType,
        SessionStatus status,
        Instant scheduledAt,
        Instant startedAt,
        Instant completedAt,
        String notes,
        Instant createdAt
) {
}
