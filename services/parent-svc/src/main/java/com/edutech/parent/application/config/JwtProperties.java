// src/main/java/com/edutech/parent/application/config/JwtProperties.java
package com.edutech.parent.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String publicKeyPath, String issuer) {}
