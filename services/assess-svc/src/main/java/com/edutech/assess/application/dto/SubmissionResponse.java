// src/main/java/com/edutech/assess/application/dto/SubmissionResponse.java
package com.edutech.assess.application.dto;

import com.edutech.assess.domain.model.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        UUID examId,
        UUID studentId,
        Instant startedAt,
        Instant submittedAt,
        double totalMarks,
        double scoredMarks,
        double percentage,
        SubmissionStatus status,
        int attemptNumber
) {}
