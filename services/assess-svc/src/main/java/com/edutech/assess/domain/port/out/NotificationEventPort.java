// src/main/java/com/edutech/assess/domain/port/out/NotificationEventPort.java
package com.edutech.assess.domain.port.out;

import com.edutech.events.notification.NotificationSendEvent;

/**
 * Outbound port for publishing notification-send events.
 * Implemented in infrastructure by NotificationKafkaPublisher.
 */
public interface NotificationEventPort {
    void publish(NotificationSendEvent event);
}
