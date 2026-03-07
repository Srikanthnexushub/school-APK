package com.edutech.examtracker.application.dto;

import com.edutech.examtracker.domain.model.SessionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudySessionResponse(
        UUID id,
        UUID studentId,
        UUID enrollmentId,
        String subject,
        String topicName,
        SessionType sessionType,
        LocalDate sessionDate,
        Integer durationMinutes,
        Integer questionsAttempted,
        BigDecimal accuracyPercent,
        String notes,
        Instant createdAt
) {}
