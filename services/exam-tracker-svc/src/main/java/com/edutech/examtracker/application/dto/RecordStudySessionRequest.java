package com.edutech.examtracker.application.dto;

import com.edutech.examtracker.domain.model.SessionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecordStudySessionRequest(
        @NotNull UUID enrollmentId,
        @NotBlank String subject,
        @NotBlank String topicName,
        @NotNull SessionType sessionType,
        @NotNull LocalDate sessionDate,
        @Min(1) Integer durationMinutes,
        Integer questionsAttempted,
        BigDecimal accuracyPercent,
        String notes
) {}
