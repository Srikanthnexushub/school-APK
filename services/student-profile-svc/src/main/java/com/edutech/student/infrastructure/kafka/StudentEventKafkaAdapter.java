package com.edutech.student.infrastructure.kafka;

import com.edutech.student.domain.port.out.StudentEventPublisher;
import com.edutech.student.infrastructure.config.KafkaTopicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class StudentEventKafkaAdapter implements StudentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(StudentEventKafkaAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public StudentEventKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                     KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publish(Object event) {
        try {
            kafkaTemplate.send(topicProperties.studentEvents(), event);
            log.debug("Published event: type={}", event.getClass().getSimpleName());
        } catch (Exception ex) {
            log.error("Failed to publish event: type={} error={}", event.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }
}
