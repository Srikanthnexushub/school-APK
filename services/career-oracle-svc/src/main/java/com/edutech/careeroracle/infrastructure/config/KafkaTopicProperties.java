package com.edutech.careeroracle.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicProperties {

    private String careerRecommended;
    private String careerProfileCreated;

    public KafkaTopicProperties() {
    }

    public String getCareerRecommended() {
        return careerRecommended;
    }

    public void setCareerRecommended(String careerRecommended) {
        this.careerRecommended = careerRecommended;
    }

    public String getCareerProfileCreated() {
        return careerProfileCreated;
    }

    public void setCareerProfileCreated(String careerProfileCreated) {
        this.careerProfileCreated = careerProfileCreated;
    }
}
