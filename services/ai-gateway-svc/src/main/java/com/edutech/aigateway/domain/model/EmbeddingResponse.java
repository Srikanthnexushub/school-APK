package com.edutech.aigateway.domain.model;

import java.util.List;

public record EmbeddingResponse(String requestId, List<Double> embedding, String modelUsed, long latencyMs) {}
