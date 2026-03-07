package com.edutech.aigateway.domain.port.out;

import com.edutech.aigateway.domain.model.EmbeddingRequest;
import com.edutech.aigateway.domain.model.EmbeddingResponse;
import reactor.core.publisher.Mono;

public interface EmbeddingClient {
    Mono<EmbeddingResponse> embed(EmbeddingRequest request);
}
