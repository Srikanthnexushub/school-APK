package com.edutech.mentorsvc.domain.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MentorSessionBookedEvent(
        UUID sessionId,
        UUID mentorId,
        UUID studentId,
        OffsetDateTime scheduledAt,
        int durationMinutes,
        String sessionMode,
        OffsetDateTime occurredAt
) {
    public static MentorSessionBookedEvent of(UUID sessionId, UUID mentorId, UUID studentId,
                                               OffsetDateTime scheduledAt, int durationMinutes,
                                               String sessionMode) {
        return new MentorSessionBookedEvent(sessionId, mentorId, studentId, scheduledAt,
                durationMinutes, sessionMode, OffsetDateTime.now());
    }
}
