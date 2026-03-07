// src/main/java/com/edutech/auth/infrastructure/config/CaptchaProperties.java
package com.edutech.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "captcha")
public record CaptchaProperties(
    String verifyUrl,
    String siteKey,
    String secretKey
) {}
