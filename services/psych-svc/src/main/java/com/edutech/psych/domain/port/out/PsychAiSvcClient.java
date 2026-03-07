package com.edutech.psych.domain.port.out;

import java.util.UUID;

public interface PsychAiSvcClient {

    CareerPredictionResponse predictCareers(UUID profileId,
                                            double openness,
                                            double conscientiousness,
                                            double extraversion,
                                            double agreeableness,
                                            double neuroticism,
                                            String riasecCode);
}
