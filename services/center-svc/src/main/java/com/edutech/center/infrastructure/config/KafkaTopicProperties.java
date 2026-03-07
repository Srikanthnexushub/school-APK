// src/main/java/com/edutech/center/infrastructure/config/KafkaTopicProperties.java
package com.edutech.center.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.topics")
public record KafkaTopicProperties(
    String centerEvents,
    String auditImmutable
) {}
