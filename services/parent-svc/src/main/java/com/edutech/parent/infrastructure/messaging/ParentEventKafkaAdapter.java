// src/main/java/com/edutech/parent/infrastructure/messaging/ParentEventKafkaAdapter.java
package com.edutech.parent.infrastructure.messaging;

import com.edutech.parent.domain.port.out.ParentEventPublisher;
import com.edutech.parent.infrastructure.config.KafkaTopicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ParentEventKafkaAdapter implements ParentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ParentEventKafkaAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public ParentEventKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                   KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publish(Object event) {
        String topic = topicProperties.parentEvents();
        kafkaTemplate.send(topic, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event: type={} error={}",
                        event.getClass().getSimpleName(), ex.getMessage());
                } else {
                    log.debug("Event published: type={} topic={} offset={}",
                        event.getClass().getSimpleName(), topic,
                        result.getRecordMetadata().offset());
                }
            });
    }
}
