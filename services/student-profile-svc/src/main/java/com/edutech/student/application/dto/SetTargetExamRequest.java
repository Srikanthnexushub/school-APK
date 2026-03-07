package com.edutech.student.application.dto;

import com.edutech.student.domain.model.ExamCode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetTargetExamRequest(
        @NotNull ExamCode examCode,
        @NotNull Integer targetYear,
        @Min(1) Integer priority
) {}
