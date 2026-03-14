// src/main/java/com/edutech/auth/infrastructure/kafka/AuditEventKafkaAdapter.java
package com.edutech.auth.infrastructure.kafka;

import com.edutech.auth.domain.port.out.AuditEventPublisher;
import com.edutech.auth.infrastructure.config.KafkaTopicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka adapter for immutable audit event publishing.
 * Publishes to the append-only audit topic (retention = forever).
 * Failures are logged but do NOT cause the calling transaction to roll back —
 * audit events are best-effort; the primary transaction must succeed.
 */
@Component
public class AuditEventKafkaAdapter implements AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditEventKafkaAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public AuditEventKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                  KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publish(Object event) {
        String topic = topicProperties.auditImmutable();
        try {
            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish audit event type={} topic={}",
                        event.getClass().getSimpleName(), topic, ex);
                } else {
                    log.debug("Audit event published type={} partition={} offset={}",
                        event.getClass().getSimpleName(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            // Kafka unavailable — log and continue; audit events are best-effort
            log.warn("Audit event dropped (Kafka unavailable) type={} topic={} reason={}",
                event.getClass().getSimpleName(), topic, e.getMessage());
        }
    }
}
