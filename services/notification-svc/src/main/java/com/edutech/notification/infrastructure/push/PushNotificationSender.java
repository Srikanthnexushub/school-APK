package com.edutech.notification.infrastructure.push;

import com.edutech.notification.domain.model.Notification;
import com.edutech.notification.domain.model.NotificationChannel;
import com.edutech.notification.domain.port.out.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub PUSH notification sender.
 *
 * <p>PUSH_NOT_IMPLEMENTED: Replace this stub with an FCM/APNs integration when
 * the mobile app SDK is available. Until then, every PUSH notification is immediately
 * marked as FAILED with message "PUSH_NOT_IMPLEMENTED".</p>
 */
@Component
public class PushNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public void send(Notification notification) {
        log.warn("PUSH delivery is not implemented — notificationId={} recipientId={}. "
                + "Replace this stub with FCM/APNs integration.",
                notification.getId(), notification.getRecipientId());
        throw new UnsupportedOperationException("PUSH_NOT_IMPLEMENTED");
    }
}
