package com.edutech.aigateway.domain.port.in;

import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.domain.model.CareerPredictionRequest;
import com.edutech.aigateway.domain.model.CareerPredictionResponse;
import reactor.core.publisher.Mono;

public interface RouteCareerPredictionUseCase {
    Mono<CareerPredictionResponse> routeCareerPrediction(CareerPredictionRequest request, AuthPrincipal principal);
}
