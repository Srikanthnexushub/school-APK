// src/main/java/com/edutech/center/application/config/JwtProperties.java
package com.edutech.center.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String publicKeyPath,
    String issuer
) {}
