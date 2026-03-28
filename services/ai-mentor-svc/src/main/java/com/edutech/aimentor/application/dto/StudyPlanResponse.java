package com.edutech.aimentor.application.dto;

import com.edutech.aimentor.domain.model.PlanType;

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
        PlanType planType,
        String board,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        List<StudyPlanItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {}
