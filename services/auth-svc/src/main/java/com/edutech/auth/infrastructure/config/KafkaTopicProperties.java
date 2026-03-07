// src/main/java/com/edutech/auth/infrastructure/config/KafkaTopicProperties.java
package com.edutech.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.topics")
public record KafkaTopicProperties(
    String authEvents,
    String auditImmutable,
    String notificationSend
) {}
