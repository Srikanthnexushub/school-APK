// src/main/java/com/edutech/parent/domain/port/out/NotificationPublisher.java
package com.edutech.parent.domain.port.out;

import java.util.Map;
import java.util.UUID;

/**
 * Port for sending notifications to users through the notification service.
 * Implementations route events to the appropriate Kafka topic.
 */
public interface NotificationPublisher {
    void sendInApp(UUID recipientUserId, String subject, String body, Map<String, String> metadata);
}
