package com.edutech.examtracker.application.dto;

import com.edutech.examtracker.domain.model.ExamCode;
import com.edutech.examtracker.domain.model.ExamStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExamEnrollmentResponse(
        UUID id,
        UUID studentId,
        ExamCode examCode,
        String examName,
        LocalDate examDate,
        Integer targetYear,
        ExamStatus status,
        Integer syllabusModulesTotal,
        Integer syllabusModulesCompleted,
        BigDecimal overallSyllabusPercent,
        Instant createdAt
) {}
