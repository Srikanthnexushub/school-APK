// src/main/java/com/edutech/assess/application/dto/AssignmentResponse.java
package com.edutech.assess.application.dto;

import com.edutech.assess.domain.model.AssignmentStatus;
import com.edutech.assess.domain.model.AssignmentType;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID batchId,
        UUID centerId,
        UUID createdByUserId,
        String title,
        String description,
        AssignmentType type,
        Instant dueDate,
        double totalMarks,
        double passingMarks,
        String instructions,
        String attachmentUrl,
        AssignmentStatus status,
        Instant createdAt,
        int submissionCount
) {}
