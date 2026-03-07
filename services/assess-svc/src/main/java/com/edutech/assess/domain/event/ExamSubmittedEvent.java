// src/main/java/com/edutech/assess/domain/event/ExamSubmittedEvent.java
package com.edutech.assess.domain.event;

import java.time.Instant;
import java.util.UUID;

public record ExamSubmittedEvent(
        UUID eventId,
        UUID submissionId,
        UUID examId,
        UUID studentId,
        double scoredMarks,
        double totalMarks,
        Instant occurredAt
) {
    public ExamSubmittedEvent(UUID submissionId, UUID examId, UUID studentId,
                               double scoredMarks, double totalMarks) {
        this(UUID.randomUUID(), submissionId, examId, studentId, scoredMarks, totalMarks, Instant.now());
    }
}
