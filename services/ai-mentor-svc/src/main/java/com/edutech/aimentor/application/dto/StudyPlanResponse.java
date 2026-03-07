package com.edutech.aimentor.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StudyPlanResponse(
        UUID id,
        UUID studentId,
        UUID enrollmentId,
        String title,
        String description,
        LocalDate targetExamDate,
        boolean active,
        List<StudyPlanItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {}
