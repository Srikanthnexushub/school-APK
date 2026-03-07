package com.edutech.performance.domain.port.in;

import com.edutech.performance.application.dto.ReadinessScoreResponse;

import java.util.List;
import java.util.UUID;

public interface GetReadinessScoreUseCase {

    ReadinessScoreResponse getLatestScore(UUID studentId, UUID enrollmentId);

    List<ReadinessScoreResponse> getScoreHistory(UUID studentId, UUID enrollmentId);
}
