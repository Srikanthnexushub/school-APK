package com.edutech.aigateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kafka.topics")
public record KafkaTopicProperties(String aiGatewayEvents, String auditImmutable) {}
