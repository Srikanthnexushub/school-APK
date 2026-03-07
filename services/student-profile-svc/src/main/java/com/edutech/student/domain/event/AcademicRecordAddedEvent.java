package com.edutech.student.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AcademicRecordAddedEvent(
        String eventId,
        UUID studentId,
        UUID academicRecordId,
        Integer classGrade,
        BigDecimal percentageScore,
        Instant occurredAt
) {}
