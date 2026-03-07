// src/main/java/com/edutech/assess/application/config/CatProperties.java
package com.edutech.assess.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cat")
public record CatProperties(
        int minQuestions,
        int maxQuestions,
        double initialTheta,
        double convergenceThreshold
) {}
