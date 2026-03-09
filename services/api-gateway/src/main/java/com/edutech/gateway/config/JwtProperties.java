package com.edutech.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jwt")
public record JwtProperties(String publicKeyPath, String issuer, String jwksUri, long jwksCacheTtlSeconds) {}
