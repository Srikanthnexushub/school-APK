package com.edutech.aigateway.api;

import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.domain.model.CareerPredictionRequest;
import com.edutech.aigateway.domain.model.CareerPredictionResponse;
import com.edutech.aigateway.domain.port.in.RouteCareerPredictionUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/ai/career-predictions")
public class CareerPredictionController {

    private final RouteCareerPredictionUseCase routeCareerPredictionUseCase;

    public CareerPredictionController(RouteCareerPredictionUseCase routeCareerPredictionUseCase) {
        this.routeCareerPredictionUseCase = routeCareerPredictionUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<CareerPredictionResponse> predict(
            @Valid @RequestBody CareerPredictionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return routeCareerPredictionUseCase.routeCareerPrediction(request, principal);
    }
}
