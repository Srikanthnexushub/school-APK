// src/main/java/com/edutech/auth/application/config/OtpProperties.java
package com.edutech.auth.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "otp")
public record OtpProperties(
    int expirySeconds,
    int maxAttempts,
    int length
) {}
