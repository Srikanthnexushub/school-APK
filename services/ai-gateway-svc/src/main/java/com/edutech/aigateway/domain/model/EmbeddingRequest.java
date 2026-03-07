package com.edutech.aigateway.domain.model;

public record EmbeddingRequest(String requesterId, String text, int dimensions) {}
