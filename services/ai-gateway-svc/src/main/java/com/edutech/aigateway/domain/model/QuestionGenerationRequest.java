package com.edutech.aigateway.domain.model;

public record QuestionGenerationRequest(
        String topic,
        String difficulty,
        int count
) {}
