// src/main/java/com/edutech/assess/infrastructure/messaging/CenterEventConsumer.java
package com.edutech.assess.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes domain events from center-svc.
 * Phase 2: react to BatchStatusChangedEvent to invalidate in-progress submissions for closed batches.
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
    }
}
