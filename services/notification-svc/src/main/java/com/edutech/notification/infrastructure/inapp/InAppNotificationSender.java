package com.edutech.notification.infrastructure.inapp;

import com.edutech.notification.domain.model.Notification;
import com.edutech.notification.domain.model.NotificationChannel;
import com.edutech.notification.domain.port.out.NotificationSender;
import com.edutech.notification.infrastructure.sse.SseEmitterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * IN_APP sender: notification is already persisted by NotificationService before this is called.
 * This sender pushes the event to any active SSE connections for the recipient (best-effort).
 * If no SSE connection is open, the notification remains available via the REST poll endpoint.
 */
@Component
public class InAppNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationSender.class);

    private final SseEmitterRegistry sseEmitterRegistry;

    public InAppNotificationSender(SseEmitterRegistry sseEmitterRegistry) {
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification) {
        // Push via SSE to any connected clients; silently continues if none connected
        sseEmitterRegistry.push(notification);
        log.debug("IN_APP notification dispatched: id={} type={} recipientId={}",
                notification.getId(), notification.getNotificationType(), notification.getRecipientId());
    }
}
