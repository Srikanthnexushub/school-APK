// src/main/java/com/edutech/assess/infrastructure/messaging/AssessEventKafkaAdapter.java
package com.edutech.assess.infrastructure.messaging;

import com.edutech.assess.domain.port.out.AssessEventPublisher;
import com.edutech.assess.infrastructure.config.KafkaTopicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AssessEventKafkaAdapter implements AssessEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AssessEventKafkaAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public AssessEventKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                   KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publish(Object event) {
        String topic = topicProperties.assessEvents();
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
