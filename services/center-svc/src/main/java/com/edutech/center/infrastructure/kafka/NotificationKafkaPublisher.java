// src/main/java/com/edutech/center/infrastructure/kafka/NotificationKafkaPublisher.java
package com.edutech.center.infrastructure.kafka;

import com.edutech.center.domain.port.out.NotificationPublisher;
import com.edutech.center.infrastructure.config.KafkaTopicProperties;
import com.edutech.events.notification.NotificationSendEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes NotificationSendEvent to the notification-send Kafka topic.
 * Best-effort: failures are logged but do not roll back the primary transaction.
 */
@Component
public class NotificationKafkaPublisher implements NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topics;

    public NotificationKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                      KafkaTopicProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    @Override
    public void publish(NotificationSendEvent event) {
        kafkaTemplate.send(topics.notificationSend(), event.eventId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish NotificationSendEvent eventId={} recipientId={}",
                            event.eventId(), event.recipientId(), ex);
                } else {
                    log.debug("NotificationSendEvent published eventId={} recipientId={} partition={} offset={}",
                            event.eventId(), event.recipientId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
    }
}
