package com.edutech.careeroracle.infrastructure.config;

import com.edutech.careeroracle.domain.service.CareerScoreCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public CareerScoreCalculator careerScoreCalculator() {
        return new CareerScoreCalculator();
    }
}
