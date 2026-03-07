package com.edutech.psych.domain.event;

import java.util.UUID;

public record SessionCompletedEvent(
        UUID sessionId,
        UUID profileId,
        UUID studentId,
        UUID centerId
) {
}
