package com.edutech.aimentor.domain.port.in;

import com.edutech.aimentor.application.dto.RecommendationResponse;

import java.util.List;
import java.util.UUID;

public interface GetRecommendationsUseCase {

    List<RecommendationResponse> getRecommendations(UUID studentId);
}
