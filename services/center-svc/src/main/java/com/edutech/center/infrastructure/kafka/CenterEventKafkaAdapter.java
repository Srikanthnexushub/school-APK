// src/main/java/com/edutech/center/infrastructure/kafka/CenterEventKafkaAdapter.java
package com.edutech.center.infrastructure.kafka;

import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.infrastructure.config.KafkaTopicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes center domain events to Kafka.
 * Best-effort: failures are logged but do not roll back the primary transaction.
 */
@Component
public class CenterEventKafkaAdapter implements CenterEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CenterEventKafkaAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public CenterEventKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                   KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publish(Object event) {
        String topic = topicProperties.centerEvents();
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish center event type={} topic={}",
                    event.getClass().getSimpleName(), topic, ex);
            } else {
                log.debug("Center event published type={} partition={} offset={}",
                    event.getClass().getSimpleName(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }
}
