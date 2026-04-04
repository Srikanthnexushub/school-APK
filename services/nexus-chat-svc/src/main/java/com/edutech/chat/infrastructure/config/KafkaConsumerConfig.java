package com.edutech.chat.infrastructure.config;

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
 * Kafka consumer configuration for nexus-chat-svc.
 *
 * <p>Hardening applied:
 * <ul>
 *   <li>DefaultErrorHandler with FixedBackOff(0, 0) — processing exceptions are logged
 *       and the offset committed so the consumer stays healthy (acts as a DLQ sink).</li>
 *   <li>String deserialization for both key and value — consumers parse JSON manually
 *       via ObjectMapper, so no JsonDeserializer wrapping is needed.</li>
 * </ul>
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Bean
    public ConsumerFactory<String, String> chatConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
            kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chatConsumerFactory());

        // DefaultErrorHandler with no retries: log the exception and commit offset.
        // Unprocessable messages are skipped (DLQ behaviour) instead of blocking the partition.
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, ex) -> log.error(
                        "Chat consumer skipped unprocessable record: topic={} partition={} offset={} error={}",
                        record.topic(), record.partition(), record.offset(), ex.getMessage()),
                new FixedBackOff(0L, 0L)   // 0 retries, 0 ms interval
        );
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
