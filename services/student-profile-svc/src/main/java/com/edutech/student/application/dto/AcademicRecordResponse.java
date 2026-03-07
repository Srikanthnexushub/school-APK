package com.edutech.student.application.dto;

import com.edutech.student.domain.model.Board;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AcademicRecordResponse(
        UUID id,
        UUID studentId,
        Integer academicYear,
        Integer classGrade,
        Board board,
        BigDecimal percentageScore,
        BigDecimal cgpa,
        List<SubjectScoreDto> subjects,
        Instant createdAt
) {}
