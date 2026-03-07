// src/main/java/com/edutech/assess/application/dto/CreateExamRequest.java
package com.edutech.assess.application.dto;

import com.edutech.assess.domain.model.ExamMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateExamRequest(
        @NotBlank String title,
        String description,
        @NotNull UUID batchId,
        @NotNull UUID centerId,
        @NotNull ExamMode mode,
        @Min(1) int durationMinutes,
        @Min(1) int maxAttempts,
        Instant startAt,
        Instant endAt,
        @DecimalMin("0.01") double totalMarks,
        @DecimalMin("0.01") double passingMarks
) {}
