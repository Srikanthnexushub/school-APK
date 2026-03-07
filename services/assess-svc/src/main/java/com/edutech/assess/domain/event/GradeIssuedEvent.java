// src/main/java/com/edutech/assess/domain/event/GradeIssuedEvent.java
package com.edutech.assess.domain.event;

import java.time.Instant;
import java.util.UUID;

public record GradeIssuedEvent(
        UUID eventId,
        UUID gradeId,
        UUID submissionId,
        UUID examId,
        UUID studentId,
        UUID batchId,
        UUID centerId,
        double percentage,
        boolean passed,
        Instant occurredAt
) {
    public GradeIssuedEvent(UUID gradeId, UUID submissionId, UUID examId, UUID studentId,
                             UUID batchId, UUID centerId, double percentage, boolean passed) {
        this(UUID.randomUUID(), gradeId, submissionId, examId, studentId,
                batchId, centerId, percentage, passed, Instant.now());
    }
}
