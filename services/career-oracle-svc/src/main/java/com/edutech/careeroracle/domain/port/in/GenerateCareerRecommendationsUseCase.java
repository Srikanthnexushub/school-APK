package com.edutech.careeroracle.domain.port.in;

import com.edutech.careeroracle.application.dto.CareerRecommendationResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface GenerateCareerRecommendationsUseCase {

    List<CareerRecommendationResponse> generateRecommendations(UUID studentId,
                                                                Map<String, java.math.BigDecimal> subjectStrengths);

    List<CareerRecommendationResponse> getActiveRecommendations(UUID studentId);
}
