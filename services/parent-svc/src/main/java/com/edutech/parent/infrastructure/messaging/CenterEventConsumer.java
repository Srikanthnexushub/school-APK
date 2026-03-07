// src/main/java/com/edutech/parent/infrastructure/messaging/CenterEventConsumer.java
package com.edutech.parent.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes domain events from center-svc.
 * Currently logs events for observability; future: trigger parent notifications.
 */
@Component
public class CenterEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CenterEventConsumer.class);

    @KafkaListener(
        topics = "${kafka.topics.center-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        properties = {
            "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"
        }
    )
    public void handleCenterEvent(String eventJson) {
        log.info("Received center-svc event: {}", eventJson);
        // TODO: Parse event type and dispatch parent notifications accordingly.
        // Phase 2: Route BatchStatusChangedEvent -> notify parents of affected students.
        // Phase 2: Route ContentUploadedEvent -> notify parents if batch content uploaded.
    }
}
