package com.edutech.mentorsvc.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicProperties {

    private String sessionBooked;
    private String sessionCompleted;
    private String feedbackSubmitted;

    public KafkaTopicProperties() {
    }

    public String getSessionBooked() {
        return sessionBooked;
    }

    public void setSessionBooked(String sessionBooked) {
        this.sessionBooked = sessionBooked;
    }

    public String getSessionCompleted() {
        return sessionCompleted;
    }

    public void setSessionCompleted(String sessionCompleted) {
        this.sessionCompleted = sessionCompleted;
    }

    public String getFeedbackSubmitted() {
        return feedbackSubmitted;
    }

    public void setFeedbackSubmitted(String feedbackSubmitted) {
        this.feedbackSubmitted = feedbackSubmitted;
    }
}
