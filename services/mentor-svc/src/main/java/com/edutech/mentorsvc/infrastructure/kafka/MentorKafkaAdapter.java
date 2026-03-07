package com.edutech.mentorsvc.infrastructure.kafka;

import com.edutech.mentorsvc.domain.event.FeedbackSubmittedEvent;
import com.edutech.mentorsvc.domain.event.MentorSessionBookedEvent;
import com.edutech.mentorsvc.domain.event.SessionCompletedEvent;
import com.edutech.mentorsvc.domain.port.out.MentorEventPublisher;
import com.edutech.mentorsvc.infrastructure.config.KafkaTopicProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MentorKafkaAdapter implements MentorEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MentorKafkaAdapter.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicProperties kafkaTopicProperties;
    private final ObjectMapper objectMapper;

    public MentorKafkaAdapter(KafkaTemplate<String, String> kafkaTemplate,
                               KafkaTopicProperties kafkaTopicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopicProperties = kafkaTopicProperties;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void publishSessionBooked(MentorSessionBookedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String topic = kafkaTopicProperties.getSessionBooked();
            kafkaTemplate.send(topic, event.sessionId().toString(), payload);
            log.info("Published MentorSessionBookedEvent to topic '{}' for session {}", topic, event.sessionId());
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize MentorSessionBookedEvent for session {}: {}",
                    event.sessionId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to serialize MentorSessionBookedEvent", ex);
        }
    }

    @Override
    public void publishSessionCompleted(SessionCompletedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String topic = kafkaTopicProperties.getSessionCompleted();
            kafkaTemplate.send(topic, event.sessionId().toString(), payload);
            log.info("Published SessionCompletedEvent to topic '{}' for session {}", topic, event.sessionId());
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize SessionCompletedEvent for session {}: {}",
                    event.sessionId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to serialize SessionCompletedEvent", ex);
        }
    }

    @Override
    public void publishFeedbackSubmitted(FeedbackSubmittedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String topic = kafkaTopicProperties.getFeedbackSubmitted();
            kafkaTemplate.send(topic, event.feedbackId().toString(), payload);
            log.info("Published FeedbackSubmittedEvent to topic '{}' for feedback {}", topic, event.feedbackId());
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize FeedbackSubmittedEvent for feedback {}: {}",
                    event.feedbackId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to serialize FeedbackSubmittedEvent", ex);
        }
    }
}
