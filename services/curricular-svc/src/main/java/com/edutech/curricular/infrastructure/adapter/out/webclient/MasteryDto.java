package com.edutech.curricular.infrastructure.adapter.out.webclient;

import java.util.Map;

public record MasteryDto(Map<String, Double> topicMasteryRatios) {

    public static MasteryDto empty() {
        return new MasteryDto(Map.of());
    }

    public double getMasteryFor(String topicCode) {
        if (topicMasteryRatios == null) return 0.0;
        return topicMasteryRatios.getOrDefault(topicCode, 0.0);
    }
}
