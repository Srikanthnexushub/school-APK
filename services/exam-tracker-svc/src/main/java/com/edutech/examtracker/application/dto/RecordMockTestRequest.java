package com.edutech.examtracker.application.dto;

import com.edutech.examtracker.domain.model.ExamCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecordMockTestRequest(
        @NotNull UUID enrollmentId,
        @NotBlank String testName,
        @NotNull ExamCode examCode,
        @NotNull LocalDate attemptDate,
        @NotNull Integer totalQuestions,
        @NotNull Integer attempted,
        @NotNull Integer correct,
        @NotNull Integer incorrect,
        @NotNull BigDecimal score,
        @NotNull BigDecimal maxScore,
        @NotNull Integer timeTakenMinutes,
        @NotNull Integer totalTimeMinutes,
        String subjectWiseJson
) {}
