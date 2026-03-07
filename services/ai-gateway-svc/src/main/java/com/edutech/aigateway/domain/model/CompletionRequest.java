package com.edutech.aigateway.domain.model;

public record CompletionRequest(
        String requesterId,
        String systemPrompt,
        String userMessage,
        int maxTokens,
        double temperature
) {}
