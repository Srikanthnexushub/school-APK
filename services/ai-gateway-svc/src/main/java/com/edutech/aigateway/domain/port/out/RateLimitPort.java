package com.edutech.aigateway.domain.port.out;

import com.edutech.aigateway.domain.model.ModelType;
import reactor.core.publisher.Mono;

public interface RateLimitPort {
    // Returns true if request is allowed, false if rate limit exceeded
    Mono<Boolean> checkAndIncrement(String requesterId, ModelType modelType);
}
