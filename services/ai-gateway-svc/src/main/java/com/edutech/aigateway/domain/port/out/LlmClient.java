package com.edutech.aigateway.domain.port.out;

import com.edutech.aigateway.domain.model.CompletionRequest;
import com.edutech.aigateway.domain.model.CompletionResponse;
import com.edutech.aigateway.domain.model.LlmProvider;
import reactor.core.publisher.Mono;

public interface LlmClient {
    Mono<CompletionResponse> complete(CompletionRequest request, LlmProvider provider);
}
