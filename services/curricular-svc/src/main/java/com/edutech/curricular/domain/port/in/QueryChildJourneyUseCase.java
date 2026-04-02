package com.edutech.curricular.domain.port.in;

import com.edutech.curricular.application.dto.ChildJourneyResponse;

import java.util.UUID;

public interface QueryChildJourneyUseCase {
    ChildJourneyResponse getChildJourney(UUID studentId, UUID parentId);
}
