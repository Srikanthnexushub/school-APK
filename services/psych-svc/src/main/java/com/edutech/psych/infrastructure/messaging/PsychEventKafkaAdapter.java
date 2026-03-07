package com.edutech.psych.infrastructure.messaging;

import com.edutech.psych.domain.event.CareerMappingGeneratedEvent;
import com.edutech.psych.domain.event.PsychProfileCreatedEvent;
import com.edutech.psych.domain.event.SessionCompletedEvent;
import com.edutech.psych.domain.port.out.PsychEventPublisher;
import com.edutech.psych.infrastructure.config.KafkaTopicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PsychEventKafkaAdapter implements PsychEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PsychEventKafkaAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public PsychEventKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                   KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publish(Object event) {
        if (event instanceof PsychProfileCreatedEvent e) {
            sendToPsychEvents(e.profileId().toString(), e);
            sendToAudit(e.profileId().toString(), e);
        } else if (event instanceof SessionCompletedEvent e) {
            sendToPsychEvents(e.sessionId().toString(), e);
            sendToAudit(e.sessionId().toString(), e);
        } else if (event instanceof CareerMappingGeneratedEvent e) {
            sendToPsychEvents(e.mappingId().toString(), e);
            sendToAudit(e.mappingId().toString(), e);
        } else {
            log.warn("Unknown event type received for publishing: {}", event.getClass().getName());
            sendToAudit(null, event);
        }
    }

    private void sendToPsychEvents(String key, Object event) {
        try {
            kafkaTemplate.send(topicProperties.psychEvents(), key, event);
            log.debug("Published event {} to topic {} with key {}", event.getClass().getSimpleName(),
                    topicProperties.psychEvents(), key);
        } catch (Exception ex) {
            log.error("Failed to publish event {} to psychEvents topic: {}",
                    event.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }

    private void sendToAudit(String key, Object event) {
        try {
            kafkaTemplate.send(topicProperties.auditImmutable(), key, event);
            log.debug("Published event {} to audit topic with key {}", event.getClass().getSimpleName(), key);
        } catch (Exception ex) {
            log.error("Failed to publish event {} to auditImmutable topic: {}",
                    event.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }
}
