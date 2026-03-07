package com.edutech.mentorsvc.domain.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FeedbackSubmittedEvent(
        UUID feedbackId,
        UUID sessionId,
        UUID mentorId,
        UUID studentId,
        int rating,
        OffsetDateTime occurredAt
) {
    public static FeedbackSubmittedEvent of(UUID feedbackId, UUID sessionId, UUID mentorId,
                                             UUID studentId, int rating) {
        return new FeedbackSubmittedEvent(feedbackId, sessionId, mentorId, studentId, rating,
                OffsetDateTime.now());
    }
}
