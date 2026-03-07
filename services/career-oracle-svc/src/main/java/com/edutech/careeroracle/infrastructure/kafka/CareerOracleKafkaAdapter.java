package com.edutech.careeroracle.infrastructure.kafka;

import com.edutech.careeroracle.domain.event.CareerProfileCreatedEvent;
import com.edutech.careeroracle.domain.event.CareerRecommendedEvent;
import com.edutech.careeroracle.domain.port.out.CareerOracleEventPublisher;
import com.edutech.careeroracle.infrastructure.config.KafkaTopicProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CareerOracleKafkaAdapter implements CareerOracleEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CareerOracleKafkaAdapter.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicProperties kafkaTopicProperties;
    private final ObjectMapper objectMapper;

    public CareerOracleKafkaAdapter(KafkaTemplate<String, String> kafkaTemplate,
                                     KafkaTopicProperties kafkaTopicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopicProperties = kafkaTopicProperties;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void publishCareerProfileCreated(CareerProfileCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = event.profileId().toString();
            kafkaTemplate.send(kafkaTopicProperties.getCareerProfileCreated(), key, payload);
            log.info("Published CareerProfileCreatedEvent for profileId={}", event.profileId());
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize CareerProfileCreatedEvent for profileId={}: {}",
                    event.profileId(), ex.getMessage());
            throw new RuntimeException("Failed to serialize event", ex);
        } catch (Exception ex) {
            log.error("Failed to publish CareerProfileCreatedEvent for profileId={}: {}",
                    event.profileId(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public void publishCareerRecommended(CareerRecommendedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = event.studentId().toString();
            kafkaTemplate.send(kafkaTopicProperties.getCareerRecommended(), key, payload);
            log.info("Published CareerRecommendedEvent for studentId={}", event.studentId());
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize CareerRecommendedEvent for studentId={}: {}",
                    event.studentId(), ex.getMessage());
            throw new RuntimeException("Failed to serialize event", ex);
        } catch (Exception ex) {
            log.error("Failed to publish CareerRecommendedEvent for studentId={}: {}",
                    event.studentId(), ex.getMessage());
            throw ex;
        }
    }
}
