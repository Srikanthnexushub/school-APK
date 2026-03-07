package com.edutech.psych.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("psych-ai-svc")
public record PsychAiSvcProperties(String baseUrl, int connectTimeoutSeconds, int readTimeoutSeconds) {
}
