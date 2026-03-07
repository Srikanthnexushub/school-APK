package com.edutech.careeroracle.domain.event;

import com.edutech.careeroracle.domain.model.CareerStream;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CareerRecommendedEvent(
        UUID studentId,
        List<CareerStream> recommendedStreams,
        CareerStream topRecommendation,
        OffsetDateTime occurredAt
) {
}
