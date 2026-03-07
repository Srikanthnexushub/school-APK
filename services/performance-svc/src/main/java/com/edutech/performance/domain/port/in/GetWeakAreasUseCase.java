package com.edutech.performance.domain.port.in;

import com.edutech.performance.application.dto.WeakAreaResponse;

import java.util.List;
import java.util.UUID;

public interface GetWeakAreasUseCase {

    List<WeakAreaResponse> getWeakAreas(UUID studentId, UUID enrollmentId);

    List<WeakAreaResponse> getTopWeakAreas(UUID studentId, UUID enrollmentId, int limit);
}
