package com.edutech.psych.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class AssessEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AssessEventConsumer.class);

    @KafkaListener(
            topics = "${kafka.topics.assess-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {"value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"}
    )
    public void handleAssessEvent(@Payload String payload) {
        log.info("Received assess event: {}", payload);
        // Phase 2: trigger periodic psych profile review if grade indicates stress patterns
    }
}
