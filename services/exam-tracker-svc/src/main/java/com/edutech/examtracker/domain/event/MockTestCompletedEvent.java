package com.edutech.examtracker.domain.event;

import com.edutech.examtracker.domain.model.ExamCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MockTestCompletedEvent(
        String eventId,
        UUID studentId,
        UUID enrollmentId,
        ExamCode examCode,
        BigDecimal score,
        BigDecimal accuracyPercent,
        Integer estimatedRank,
        LocalDate attemptDate,
        Instant occurredAt
) {}
