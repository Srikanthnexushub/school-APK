package com.edutech.examtracker.application.dto;

import com.edutech.examtracker.domain.model.ExamCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EnrollInExamRequest(
        @NotNull ExamCode examCode,
        @NotBlank String examName,
        @NotNull Integer targetYear,
        LocalDate examDate
) {}
