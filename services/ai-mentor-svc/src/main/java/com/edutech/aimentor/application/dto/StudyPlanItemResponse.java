package com.edutech.aimentor.application.dto;

import com.edutech.aimentor.domain.model.PriorityLevel;
import com.edutech.aimentor.domain.model.SubjectArea;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudyPlanItemResponse(
        UUID id,
        SubjectArea subjectArea,
        String topic,
        PriorityLevel priorityLevel,
        int interval,
        int repetitions,
        BigDecimal easeFactor,
        LocalDate nextReviewAt,
        LocalDate lastReviewedAt,
        Integer quality,
        Instant createdAt
) {}
