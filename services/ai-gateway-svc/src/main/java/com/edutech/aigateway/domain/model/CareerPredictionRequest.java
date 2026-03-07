package com.edutech.aigateway.domain.model;

public record CareerPredictionRequest(
        String requesterId,
        String profileId,
        double openness,
        double conscientiousness,
        double extraversion,
        double agreeableness,
        double neuroticism,
        String riasecCode
) {}
