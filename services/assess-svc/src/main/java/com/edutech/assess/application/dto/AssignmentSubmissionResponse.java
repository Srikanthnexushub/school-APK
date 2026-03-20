// src/main/java/com/edutech/assess/application/dto/AssignmentSubmissionResponse.java
package com.edutech.assess.application.dto;

import com.edutech.assess.domain.model.AssignmentSubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record AssignmentSubmissionResponse(
        UUID id,
        UUID assignmentId,
        UUID studentId,
        String textResponse,
        Double score,
        String feedback,
        AssignmentSubmissionStatus status,
        Instant submittedAt,
        Instant gradedAt,
        Instant createdAt
) {}
