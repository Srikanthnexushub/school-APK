package com.edutech.aigateway.domain.model;

import java.util.List;

public record CareerPredictionResponse(
        String requestId,
        List<String> topCareers,
        String reasoning,
        String modelVersion,
        long latencyMs
) {}
