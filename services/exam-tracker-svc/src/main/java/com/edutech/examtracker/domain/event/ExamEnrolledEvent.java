package com.edutech.examtracker.domain.event;

import com.edutech.examtracker.domain.model.ExamCode;

import java.time.Instant;
import java.util.UUID;

public record ExamEnrolledEvent(
        String eventId,
        UUID studentId,
        UUID enrollmentId,
        ExamCode examCode,
        Integer targetYear,
        Instant occurredAt
) {}
