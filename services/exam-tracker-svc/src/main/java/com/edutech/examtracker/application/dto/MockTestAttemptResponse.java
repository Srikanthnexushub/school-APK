package com.edutech.examtracker.application.dto;

import com.edutech.examtracker.domain.model.ExamCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MockTestAttemptResponse(
        UUID id,
        UUID studentId,
        String testName,
        ExamCode examCode,
        LocalDate attemptDate,
        Integer correct,
        Integer incorrect,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal accuracyPercent,
        Integer timeTakenMinutes,
        Integer estimatedRank,
        Instant createdAt
) {}
