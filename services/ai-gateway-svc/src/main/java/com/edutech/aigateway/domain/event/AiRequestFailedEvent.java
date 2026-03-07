package com.edutech.aigateway.domain.event;

import com.edutech.aigateway.domain.model.ModelType;

public record AiRequestFailedEvent(
        String requestId,
        String requesterId,
        ModelType modelType,
        String errorMessage
) {}
