package com.edutech.performance.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kafka.consumer")
public record KafkaConsumerProperties(
        String studentEventsGroupId,
        String examEventsGroupId
) {
}
