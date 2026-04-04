// src/main/java/com/edutech/auth/infrastructure/config/AuditKafkaConsumerConfig.java
package com.edutech.auth.infrastructure.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration for the auth-svc audit-immutable consumer.
 *
 * Hardening applied:
 * <ul>
 *   <li>Plain String deserialiser — the audit-immutable topic carries raw JSON strings
 *       produced by {@link com.edutech.auth.infrastructure.kafka.AuditEventKafkaAdapter}
 *       which uses a generic {@code KafkaTemplate<String, Object>}. Consuming as String
 *       avoids class-not-found issues with heterogeneous event types.</li>
 *   <li>DefaultErrorHandler with FixedBackOff(0, 0) — a poison-pill message is logged
 *       and the offset committed so the consumer stays healthy. This is the DLQ-lite
 *       pattern used across the platform (see notification-svc KafkaConsumerConfig).</li>
 * </ul>
 */
@Configuration
public class AuditKafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(AuditKafkaConsumerConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Bean
    public ConsumerFactory<String, String> auditEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "auth-svc-audit-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
            auditEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(auditEventConsumerFactory());

        // DefaultErrorHandler with no retries: log and commit offset on failure.
        // Prevents an unprocessable audit record from blocking the partition.
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, ex) -> log.error(
                        "Audit consumer skipped unprocessable record: " +
                        "topic={} partition={} offset={} error={}",
                        record.topic(), record.partition(), record.offset(), ex.getMessage()),
                new FixedBackOff(0L, 0L)   // 0 retries, 0 ms interval
        );
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
