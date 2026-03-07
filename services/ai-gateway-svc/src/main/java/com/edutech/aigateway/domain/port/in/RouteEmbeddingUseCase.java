package com.edutech.aigateway.domain.port.in;

import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.domain.model.EmbeddingRequest;
import com.edutech.aigateway.domain.model.EmbeddingResponse;
import reactor.core.publisher.Mono;

public interface RouteEmbeddingUseCase {
    Mono<EmbeddingResponse> routeEmbedding(EmbeddingRequest request, AuthPrincipal principal);
}
