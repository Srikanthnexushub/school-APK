package com.edutech.performance.domain.port.in;

import com.edutech.performance.application.dto.ReadinessScoreResponse;

import java.util.UUID;

public interface ComputeReadinessScoreUseCase {

    ReadinessScoreResponse computeScore(UUID studentId, UUID enrollmentId);
}
