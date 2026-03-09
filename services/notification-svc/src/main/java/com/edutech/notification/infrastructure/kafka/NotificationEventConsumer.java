package com.edutech.notification.infrastructure.kafka;

import com.edutech.events.notification.NotificationSendEvent;
import com.edutech.notification.application.dto.NotificationCommand;
import com.edutech.notification.domain.port.in.SendNotificationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link NotificationSendEvent} from the notification-send Kafka topic
 * and delegates to {@link SendNotificationUseCase} for delivery.
 */
@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final SendNotificationUseCase sendNotificationUseCase;

    public NotificationEventConsumer(SendNotificationUseCase sendNotificationUseCase) {
        this.sendNotificationUseCase = sendNotificationUseCase;
    }

    @KafkaListener(topics = "${kafka.topics.notification-send}",
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "notificationKafkaListenerContainerFactory")
    public void onNotificationSendEvent(NotificationSendEvent event) {
        log.debug("Received NotificationSendEvent: eventId={} channel={} recipientId={}",
                event.eventId(), event.channel(), event.recipientId());

        NotificationCommand command = new NotificationCommand(
                event.channel(),
                event.recipientId(),
                event.recipientEmail(),
                event.subject(),
                event.body(),
                event.metadata()
        );

        try {
            sendNotificationUseCase.send(command);
        } catch (Exception ex) {
            log.error("Failed to process NotificationSendEvent eventId={}: {}",
                    event.eventId(), ex.getMessage(), ex);
        }
    }
}
