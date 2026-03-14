package com.edutech.auth.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mfa")
public record MfaProperties(
    String issuer,
    int pendingTokenTtlSeconds,
    int resetTokenTtlSeconds
) {}
