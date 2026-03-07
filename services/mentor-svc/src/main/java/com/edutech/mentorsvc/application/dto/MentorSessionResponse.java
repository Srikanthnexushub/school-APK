package com.edutech.mentorsvc.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MentorSessionResponse(
        UUID id,
        UUID mentorId,
        String mentorName,
        UUID studentId,
        OffsetDateTime scheduledAt,
        int durationMinutes,
        String sessionMode,
        String status,
        String meetingLink,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt
) {
}
