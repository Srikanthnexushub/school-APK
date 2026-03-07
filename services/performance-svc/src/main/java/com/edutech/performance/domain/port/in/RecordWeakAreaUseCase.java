package com.edutech.performance.domain.port.in;

import com.edutech.performance.application.dto.RecordWeakAreaRequest;
import com.edutech.performance.application.dto.WeakAreaResponse;

import java.util.UUID;

public interface RecordWeakAreaUseCase {

    WeakAreaResponse recordWeakArea(UUID studentId, RecordWeakAreaRequest request);
}
