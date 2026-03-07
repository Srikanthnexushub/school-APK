// src/main/java/com/edutech/assess/application/config/JwtProperties.java
package com.edutech.assess.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String publicKeyPath, String issuer) {}
