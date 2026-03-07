// src/main/java/com/edutech/parent/infrastructure/config/KafkaTopicProperties.java
package com.edutech.parent.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.topics")
public record KafkaTopicProperties(
    String parentEvents,
    String centerEvents,
    String auditImmutable
) {}
