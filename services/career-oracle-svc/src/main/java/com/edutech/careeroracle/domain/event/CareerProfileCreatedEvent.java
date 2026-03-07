package com.edutech.careeroracle.domain.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CareerProfileCreatedEvent(
        UUID profileId,
        UUID studentId,
        UUID enrollmentId,
        String academicStream,
        Integer currentGrade,
        OffsetDateTime occurredAt
) {
}
