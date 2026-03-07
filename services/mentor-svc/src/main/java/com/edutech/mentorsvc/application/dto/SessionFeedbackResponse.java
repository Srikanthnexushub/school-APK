package com.edutech.mentorsvc.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionFeedbackResponse(
        UUID id,
        UUID sessionId,
        UUID studentId,
        UUID mentorId,
        int rating,
        String comment,
        OffsetDateTime createdAt
) {
}
