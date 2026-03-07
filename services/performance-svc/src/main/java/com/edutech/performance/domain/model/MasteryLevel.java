package com.edutech.performance.domain.model;

import java.math.BigDecimal;

public enum MasteryLevel {
    BEGINNER,
    DEVELOPING,
    PROFICIENT,
    MASTERED;

    public static MasteryLevel from(BigDecimal masteryPercent) {
        if (masteryPercent == null) {
            return BEGINNER;
        }
        double val = masteryPercent.doubleValue();
        if (val <= 30.0) {
            return BEGINNER;
        } else if (val <= 60.0) {
            return DEVELOPING;
        } else if (val <= 80.0) {
            return PROFICIENT;
        } else {
            return MASTERED;
        }
    }
}
