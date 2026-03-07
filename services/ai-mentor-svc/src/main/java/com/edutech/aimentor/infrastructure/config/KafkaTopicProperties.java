package com.edutech.aimentor.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kafka.topics")
public record KafkaTopicProperties(
        String studyPlanCreated,
        String doubtSubmitted,
        String doubtResolved
) {}
