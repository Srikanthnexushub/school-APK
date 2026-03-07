package com.edutech.aigateway.application.exception;

import com.edutech.aigateway.domain.model.ModelType;

public class RateLimitExceededException extends AiGatewayException {

    public RateLimitExceededException(String requesterId, ModelType modelType) {
        super("Rate limit exceeded for requester: " + requesterId + " on model: " + modelType);
    }
}
