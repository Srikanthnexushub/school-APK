// src/main/java/com/edutech/auth/infrastructure/config/CleanupProperties.java
package com.edutech.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.cleanup")
public record CleanupProperties(String cron, int retentionHours) {}
