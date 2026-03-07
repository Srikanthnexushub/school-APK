package com.edutech.mentorsvc.domain.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionCompletedEvent(
        UUID sessionId,
        UUID mentorId,
        UUID studentId,
        OffsetDateTime completedAt,
        OffsetDateTime occurredAt
) {
    public static SessionCompletedEvent of(UUID sessionId, UUID mentorId, UUID studentId,
                                            OffsetDateTime completedAt) {
        return new SessionCompletedEvent(sessionId, mentorId, studentId, completedAt, OffsetDateTime.now());
    }
}
