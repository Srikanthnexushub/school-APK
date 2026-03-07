package com.edutech.psych.domain.event;

import java.util.UUID;

public record CareerMappingGeneratedEvent(
        UUID mappingId,
        UUID profileId,
        UUID studentId,
        UUID centerId,
        String topCareers
) {
}
