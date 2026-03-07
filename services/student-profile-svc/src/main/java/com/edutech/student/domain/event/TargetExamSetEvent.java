package com.edutech.student.domain.event;

import com.edutech.student.domain.model.ExamCode;

import java.time.Instant;
import java.util.UUID;

public record TargetExamSetEvent(
        String eventId,
        UUID studentId,
        ExamCode examCode,
        Integer targetYear,
        Instant occurredAt
) {}
