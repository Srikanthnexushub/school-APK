package com.edutech.aimentor.domain.event;

import java.time.Instant;
import java.util.UUID;

public record StudyPlanCreatedEvent(
        UUID studyPlanId,
        UUID studentId,
        UUID enrollmentId,
        String title,
        Instant occurredAt
) {}
