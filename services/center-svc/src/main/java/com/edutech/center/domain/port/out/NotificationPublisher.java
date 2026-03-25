// src/main/java/com/edutech/center/domain/port/out/NotificationPublisher.java
package com.edutech.center.domain.port.out;

import com.edutech.events.notification.NotificationSendEvent;

public interface NotificationPublisher {
    void publish(NotificationSendEvent event);
}
