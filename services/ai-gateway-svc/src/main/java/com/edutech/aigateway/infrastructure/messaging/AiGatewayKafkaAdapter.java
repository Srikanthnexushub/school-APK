package com.edutech.aigateway.infrastructure.messaging;

import com.edutech.aigateway.domain.event.AiRequestFailedEvent;
import com.edutech.aigateway.domain.event.AiRequestRoutedEvent;
import com.edutech.aigateway.domain.port.out.AiGatewayEventPublisher;
import com.edutech.aigateway.infrastructure.config.KafkaTopicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AiGatewayKafkaAdapter implements AiGatewayEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayKafkaAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topics;

    public AiGatewayKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                  KafkaTopicProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    @Override
    public void publish(Object event) {
        if (event instanceof AiRequestRoutedEvent routed) {
            sendBestEffort(topics.aiGatewayEvents(), routed.requestId(), routed);
            sendBestEffort(topics.auditImmutable(), routed.requestId(), routed);
        } else if (event instanceof AiRequestFailedEvent failed) {
            sendBestEffort(topics.aiGatewayEvents(), failed.requestId(), failed);
            sendBestEffort(topics.auditImmutable(), failed.requestId(), failed);
        } else {
            log.warn("AiGatewayKafkaAdapter: unrecognised event type [{}], skipping publish",
                    event == null ? "null" : event.getClass().getName());
        }
    }

    private void sendBestEffort(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event to topic [{}] with key [{}]: {}",
                            topic, key, ex.getMessage(), ex);
                } else {
                    log.debug("Published event to topic [{}] offset [{}]",
                            topic, result.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            // Best-effort: log but never propagate
            log.error("Unexpected error while sending event to topic [{}] with key [{}]: {}",
                    topic, key, ex.getMessage(), ex);
        }
    }
}
