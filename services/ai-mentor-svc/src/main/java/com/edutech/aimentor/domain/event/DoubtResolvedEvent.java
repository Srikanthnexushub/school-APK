package com.edutech.aimentor.domain.event;

import java.time.Instant;
import java.util.UUID;

public record DoubtResolvedEvent(
        UUID doubtTicketId,
        UUID studentId,
        UUID enrollmentId,
        Instant resolvedAt,
        Instant occurredAt
) {}
