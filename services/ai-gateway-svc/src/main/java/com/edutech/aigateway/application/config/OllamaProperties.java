package com.edutech.aigateway.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ollama")
public record OllamaProperties(String baseUrl) {}
