package com.edutech.aimentor.domain.event;

import com.edutech.aimentor.domain.model.SubjectArea;

import java.time.Instant;
import java.util.UUID;

public record DoubtSubmittedEvent(
        UUID doubtTicketId,
        UUID studentId,
        UUID enrollmentId,
        SubjectArea subjectArea,
        Instant occurredAt
) {}
