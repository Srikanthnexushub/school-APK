package com.edutech.auth.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.cleanup")
public record CleanupProperties(String cron, int retentionHours) {}
