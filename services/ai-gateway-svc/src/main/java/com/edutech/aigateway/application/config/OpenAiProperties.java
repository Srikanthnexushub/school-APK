package com.edutech.aigateway.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("openai")
public record OpenAiProperties(String apiKey, String embeddingModel) {}
