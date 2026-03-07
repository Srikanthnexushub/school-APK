package com.edutech.aimentor.infrastructure.kafka;

import com.edutech.aimentor.domain.event.DoubtResolvedEvent;
import com.edutech.aimentor.domain.event.DoubtSubmittedEvent;
import com.edutech.aimentor.domain.event.StudyPlanCreatedEvent;
import com.edutech.aimentor.domain.port.out.AiMentorEventPublisher;
import com.edutech.aimentor.infrastructure.config.KafkaTopicProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AiMentorKafkaAdapter implements AiMentorEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AiMentorKafkaAdapter.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;
    private final ObjectMapper objectMapper;

    public AiMentorKafkaAdapter(KafkaTemplate<String, String> kafkaTemplate,
                                 KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void publishStudyPlanCreated(StudyPlanCreatedEvent event) {
        publish(topicProperties.studyPlanCreated(), event.studyPlanId().toString(), event);
    }

    @Override
    public void publishDoubtSubmitted(DoubtSubmittedEvent event) {
        publish(topicProperties.doubtSubmitted(), event.doubtTicketId().toString(), event);
    }

    @Override
    public void publishDoubtResolved(DoubtResolvedEvent event) {
        publish(topicProperties.doubtResolved(), event.doubtTicketId().toString(), event);
    }

    private void publish(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload);
            log.debug("Published event to topic={} key={}", topic, key);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for topic={} key={}: {}", topic, key, e.getMessage(), e);
            throw new RuntimeException("Failed to serialize Kafka event", e);
        }
    }
}
