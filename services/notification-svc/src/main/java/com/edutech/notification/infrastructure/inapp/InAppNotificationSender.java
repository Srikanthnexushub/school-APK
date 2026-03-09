package com.edutech.notification.infrastructure.inapp;

import com.edutech.notification.domain.model.Notification;
import com.edutech.notification.domain.model.NotificationChannel;
import com.edutech.notification.domain.port.out.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub IN_APP notification sender.
 *
 * <p>IN_APP_NOT_IMPLEMENTED: Replace this stub with a WebSocket or SSE push
 * to the connected React frontend when the real-time infrastructure is ready.</p>
 */
@Component
public class InAppNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification) {
        log.warn("IN_APP delivery is not implemented — notificationId={} recipientId={}. "
                + "Replace this stub with WebSocket/SSE push to the React frontend.",
                notification.getId(), notification.getRecipientId());
        throw new UnsupportedOperationException("IN_APP_NOT_IMPLEMENTED");
    }
}
