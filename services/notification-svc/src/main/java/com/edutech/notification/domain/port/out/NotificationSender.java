package com.edutech.notification.domain.port.out;

import com.edutech.notification.domain.model.Notification;
import com.edutech.notification.domain.model.NotificationChannel;

public interface NotificationSender {
    /**
     * Returns the channel this sender handles.
     */
    NotificationChannel channel();

    /**
     * Delivers the notification. Implementations must throw an exception on failure.
     */
    void send(Notification notification);
}
