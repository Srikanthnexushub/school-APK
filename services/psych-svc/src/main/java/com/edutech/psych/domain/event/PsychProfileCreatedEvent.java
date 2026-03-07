package com.edutech.psych.domain.event;

import java.util.UUID;

public record PsychProfileCreatedEvent(
        UUID profileId,
        UUID studentId,
        UUID centerId,
        UUID batchId
) {
}
