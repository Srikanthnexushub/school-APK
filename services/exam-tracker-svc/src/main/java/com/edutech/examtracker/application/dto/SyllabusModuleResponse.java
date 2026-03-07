package com.edutech.examtracker.application.dto;

import com.edutech.examtracker.domain.model.ModuleStatus;

import java.time.Instant;
import java.util.UUID;

public record SyllabusModuleResponse(
        UUID id,
        UUID enrollmentId,
        String subject,
        String topicName,
        String chapterName,
        Integer weightagePercent,
        ModuleStatus status,
        Integer completionPercent,
        Instant lastStudiedAt,
        Instant createdAt
) {}
