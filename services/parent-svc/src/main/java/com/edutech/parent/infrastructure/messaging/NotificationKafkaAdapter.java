// src/main/java/com/edutech/parent/infrastructure/messaging/NotificationKafkaAdapter.java
package com.edutech.parent.infrastructure.messaging;

import com.edutech.events.notification.NotificationSendEvent;
import com.edutech.parent.domain.port.out.NotificationPublisher;
import com.edutech.parent.infrastructure.config.KafkaTopicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class NotificationKafkaAdapter implements NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public NotificationKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                     KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    public void sendInApp(UUID recipientUserId, String subject, String body, Map<String, String> metadata) {
        NotificationSendEvent event = NotificationSendEvent.inApp(recipientUserId, subject, body, metadata);
        kafkaTemplate.send(topicProperties.notificationSend(), recipientUserId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send IN_APP notification: recipientUserId={} error={}",
                                recipientUserId, ex.getMessage());
                    } else {
                        log.debug("IN_APP notification sent: recipientUserId={}", recipientUserId);
                    }
                });
    }
}
