package com.edutech.performance.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kafka.topics")
public record KafkaTopicProperties(
        String performanceEvents,
        String auditImmutable,
        String examEvents,
        String studentEvents
) {
}
