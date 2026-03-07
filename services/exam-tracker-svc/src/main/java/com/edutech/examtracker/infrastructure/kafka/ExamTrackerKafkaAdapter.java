package com.edutech.examtracker.infrastructure.kafka;

import com.edutech.examtracker.domain.port.out.ExamTrackerEventPublisher;
import com.edutech.examtracker.infrastructure.config.KafkaTopicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes exam-tracker domain events to Kafka.
 * Significant events are sent to both the domain topic and the audit-immutable topic.
 * Best-effort: failures are logged but do not roll back the primary transaction.
 */
@Component
public class ExamTrackerKafkaAdapter implements ExamTrackerEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ExamTrackerKafkaAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public ExamTrackerKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                   KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publish(Object event) {
        sendToTopic(topicProperties.examEvents(), event);
        sendToTopic(topicProperties.auditImmutable(), event);
    }

    private void sendToTopic(String topic, Object event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish exam-tracker event type={} topic={}",
                        event.getClass().getSimpleName(), topic, ex);
            } else {
                log.debug("Exam-tracker event published type={} topic={} partition={} offset={}",
                        event.getClass().getSimpleName(),
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
