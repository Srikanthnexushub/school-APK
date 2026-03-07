package com.edutech.psych.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kafka.topics")
public record KafkaTopicProperties(String psychEvents, String assessEvents, String auditImmutable) {}
