package com.edutech.aigateway.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("anthropic")
public record AnthropicProperties(String apiKey, String model, String baseUrl) {}
