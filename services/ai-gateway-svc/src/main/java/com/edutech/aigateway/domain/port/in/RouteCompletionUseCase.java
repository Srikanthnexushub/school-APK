package com.edutech.aigateway.domain.port.in;

import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.domain.model.CompletionRequest;
import com.edutech.aigateway.domain.model.CompletionResponse;
import reactor.core.publisher.Mono;

public interface RouteCompletionUseCase {
    Mono<CompletionResponse> routeCompletion(CompletionRequest request, AuthPrincipal principal);
}
