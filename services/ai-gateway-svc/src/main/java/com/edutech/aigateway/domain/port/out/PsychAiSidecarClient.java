package com.edutech.aigateway.domain.port.out;

import com.edutech.aigateway.domain.model.CareerPredictionRequest;
import com.edutech.aigateway.domain.model.CareerPredictionResponse;
import reactor.core.publisher.Mono;

public interface PsychAiSidecarClient {
    Mono<CareerPredictionResponse> predictCareers(CareerPredictionRequest request);
}
