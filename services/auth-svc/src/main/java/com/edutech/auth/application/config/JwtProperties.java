// src/main/java/com/edutech/auth/application/config/JwtProperties.java
package com.edutech.auth.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String privateKeyPath,
    String publicKeyPath,
    long accessTokenExpirySeconds,
    long refreshTokenExpirySeconds,
    String issuer
) {}
