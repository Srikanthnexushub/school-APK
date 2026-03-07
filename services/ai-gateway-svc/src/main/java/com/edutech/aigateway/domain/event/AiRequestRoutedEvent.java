package com.edutech.aigateway.domain.event;

import com.edutech.aigateway.domain.model.LlmProvider;
import com.edutech.aigateway.domain.model.ModelType;

public record AiRequestRoutedEvent(
        String requestId,
        String requesterId,
        ModelType modelType,
        LlmProvider provider,
        long latencyMs
) {}
