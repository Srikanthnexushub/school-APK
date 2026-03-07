package com.edutech.examtracker.domain.event;

import com.edutech.examtracker.domain.model.ModuleStatus;

import java.time.Instant;
import java.util.UUID;

public record SyllabusModuleUpdatedEvent(
        String eventId,
        UUID studentId,
        UUID moduleId,
        String topicName,
        Integer completionPercent,
        ModuleStatus status,
        Instant occurredAt
) {}
