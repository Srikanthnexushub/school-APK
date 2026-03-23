// src/main/java/com/edutech/assess/application/dto/CreateAssignmentRequest.java
package com.edutech.assess.application.dto;

import com.edutech.assess.domain.model.AssignmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateAssignmentRequest(
        @NotNull UUID batchId,
        @NotNull UUID centerId,
        @NotBlank String title,
        String description,
        @NotNull AssignmentType type,
        Instant dueDate,
        Double totalMarks,
        Double passingMarks,
        String instructions,
        String attachmentUrl
) {}
