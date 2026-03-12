// src/main/java/com/edutech/assess/infrastructure/messaging/NotificationKafkaPublisher.java
package com.edutech.assess.infrastructure.messaging;

import com.edutech.assess.domain.port.out.NotificationEventPort;
import com.edutech.assess.infrastructure.config.KafkaTopicProperties;
import com.edutech.events.notification.NotificationSendEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes NotificationSendEvent to the notification-send topic consumed by notification-svc.
 */
@Component
public class NotificationKafkaPublisher implements NotificationEventPort {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public NotificationKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                       KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publish(NotificationSendEvent event) {
        String topic = topicProperties.notificationSend();
        kafkaTemplate.send(topic, event.recipientId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish NotificationSendEvent: channel={} recipientId={} error={}",
                            event.channel(), event.recipientId(), ex.getMessage());
                } else {
                    log.debug("NotificationSendEvent published: channel={} recipientId={} topic={}",
                            event.channel(), event.recipientId(), topic);
                }
            });
    }
}
