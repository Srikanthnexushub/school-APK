package com.edutech.examtracker.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StudySessionRecordedEvent(
        String eventId,
        UUID studentId,
        UUID enrollmentId,
        String subject,
        String topicName,
        Integer durationMinutes,
        BigDecimal accuracyPercent,
        Instant occurredAt
) {}
