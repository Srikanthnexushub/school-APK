package com.edutech.student.application.dto;

import com.edutech.student.domain.model.ExamCode;

import java.time.Instant;
import java.util.UUID;

public record TargetExamResponse(
        UUID id,
        UUID studentId,
        ExamCode examCode,
        Integer targetYear,
        Integer priority,
        Instant createdAt
) {}
