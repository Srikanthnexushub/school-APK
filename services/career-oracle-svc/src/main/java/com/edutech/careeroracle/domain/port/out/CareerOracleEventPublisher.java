package com.edutech.careeroracle.domain.port.out;

import com.edutech.careeroracle.domain.event.CareerProfileCreatedEvent;
import com.edutech.careeroracle.domain.event.CareerRecommendedEvent;

public interface CareerOracleEventPublisher {

    void publishCareerProfileCreated(CareerProfileCreatedEvent event);

    void publishCareerRecommended(CareerRecommendedEvent event);
}
