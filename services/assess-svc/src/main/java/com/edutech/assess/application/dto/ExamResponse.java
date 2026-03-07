// src/main/java/com/edutech/assess/application/dto/ExamResponse.java
package com.edutech.assess.application.dto;

import com.edutech.assess.domain.model.ExamMode;
import com.edutech.assess.domain.model.ExamStatus;

import java.time.Instant;
import java.util.UUID;

public record ExamResponse(
        UUID id,
        UUID batchId,
        UUID centerId,
        String title,
        String description,
        ExamMode mode,
        int durationMinutes,
        int maxAttempts,
        Instant startAt,
        Instant endAt,
        double totalMarks,
        double passingMarks,
        ExamStatus status,
        Instant createdAt
) {}
