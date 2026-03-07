package com.edutech.aigateway.domain.model;

public record CompletionResponse(
        String requestId,
        String content,
        LlmProvider provider,
        String modelUsed,
        int inputTokens,
        int outputTokens,
        long latencyMs
) {}
