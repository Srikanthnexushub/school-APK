// src/main/java/com/edutech/auth/infrastructure/config/Argon2Properties.java
package com.edutech.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "argon2")
public record Argon2Properties(
    int memoryCost,
    int iterations,
    int parallelism,
    int saltLength,
    int hashLength
) {}
