// src/main/java/com/edutech/assess/infrastructure/config/KafkaTopicProperties.java
package com.edutech.assess.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.topics")
public record KafkaTopicProperties(
    String assessEvents,
    String centerEvents,
    String auditImmutable,
    String notificationSend
) {}
