package com.edutech.curricular.domain.port.in;

import com.edutech.curricular.application.dto.CoverageLogRequest;
import com.edutech.curricular.domain.model.CoverageLog;

import java.util.UUID;

public interface LogCoverageUseCase {
    CoverageLog logCoverage(UUID teacherId, UUID centerId, CoverageLogRequest request);
}
