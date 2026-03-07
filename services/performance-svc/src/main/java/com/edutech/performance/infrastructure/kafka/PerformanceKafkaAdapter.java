package com.edutech.performance.infrastructure.kafka;

import com.edutech.performance.domain.event.DropoutRiskEscalatedEvent;
import com.edutech.performance.domain.event.ReadinessScoreUpdatedEvent;
import com.edutech.performance.domain.event.WeakAreaDetectedEvent;
import com.edutech.performance.domain.port.out.PerformanceEventPublisher;
import com.edutech.performance.infrastructure.config.KafkaTopicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PerformanceKafkaAdapter implements PerformanceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PerformanceKafkaAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public PerformanceKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                    KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publish(Object event) {
        if (event instanceof ReadinessScoreUpdatedEvent e) {
            sendToPerformanceEvents(e.studentId().toString(), e);
            sendToAudit(e.eventId(), e);
        } else if (event instanceof WeakAreaDetectedEvent e) {
            sendToPerformanceEvents(e.studentId().toString(), e);
            sendToAudit(e.eventId(), e);
        } else if (event instanceof DropoutRiskEscalatedEvent e) {
            sendToPerformanceEvents(e.studentId().toString(), e);
            sendToAudit(e.eventId(), e);
        } else {
            log.warn("Unknown event type received for publishing: {}", event.getClass().getName());
            sendToAudit(null, event);
        }
    }

    private void sendToPerformanceEvents(String key, Object event) {
        try {
            kafkaTemplate.send(topicProperties.performanceEvents(), key, event);
            log.debug("Published event {} to topic {} with key {}",
                    event.getClass().getSimpleName(), topicProperties.performanceEvents(), key);
        } catch (Exception ex) {
            log.error("Failed to publish event {} to performanceEvents topic: {}",
                    event.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }

    private void sendToAudit(String key, Object event) {
        try {
            kafkaTemplate.send(topicProperties.auditImmutable(), key, event);
            log.debug("Published event {} to audit topic with key {}",
                    event.getClass().getSimpleName(), key);
        } catch (Exception ex) {
            log.error("Failed to publish event {} to auditImmutable topic: {}",
                    event.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }
}
