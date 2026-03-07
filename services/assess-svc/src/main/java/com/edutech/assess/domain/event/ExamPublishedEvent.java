// src/main/java/com/edutech/assess/domain/event/ExamPublishedEvent.java
package com.edutech.assess.domain.event;

import java.time.Instant;
import java.util.UUID;

public record ExamPublishedEvent(
        UUID eventId,
        UUID examId,
        UUID batchId,
        UUID centerId,
        String title,
        double totalMarks,
        Instant occurredAt
) {
    public ExamPublishedEvent(UUID examId, UUID batchId, UUID centerId, String title, double totalMarks) {
        this(UUID.randomUUID(), examId, batchId, centerId, title, totalMarks, Instant.now());
    }
}
