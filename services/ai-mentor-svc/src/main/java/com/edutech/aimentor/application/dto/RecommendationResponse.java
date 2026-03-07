package com.edutech.aimentor.application.dto;

import com.edutech.aimentor.domain.model.PriorityLevel;
import com.edutech.aimentor.domain.model.SubjectArea;

import java.time.Instant;
import java.util.UUID;

public record RecommendationResponse(
        UUID id,
        UUID studentId,
        UUID enrollmentId,
        SubjectArea subjectArea,
        String topic,
        String recommendationText,
        PriorityLevel priorityLevel,
        boolean acknowledged,
        Instant createdAt,
        Instant expiresAt
) {}
