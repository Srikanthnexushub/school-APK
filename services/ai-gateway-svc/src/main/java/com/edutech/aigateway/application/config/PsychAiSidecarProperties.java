package com.edutech.aigateway.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sidecars.psych-ai")
public record PsychAiSidecarProperties(String baseUrl, int connectTimeoutSeconds, int readTimeoutSeconds) {}
