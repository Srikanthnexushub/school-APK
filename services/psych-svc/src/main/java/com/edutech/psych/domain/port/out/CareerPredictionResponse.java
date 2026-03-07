package com.edutech.psych.domain.port.out;

import java.util.List;

public record CareerPredictionResponse(
        List<String> topCareers,
        String reasoning,
        String modelVersion
) {
}
