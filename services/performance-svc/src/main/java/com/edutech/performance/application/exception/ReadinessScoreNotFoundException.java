package com.edutech.performance.application.exception;

import java.util.UUID;

public class ReadinessScoreNotFoundException extends PerformanceException {

    public ReadinessScoreNotFoundException(UUID studentId, UUID enrollmentId) {
        super("No readiness score found for studentId=" + studentId + " enrollmentId=" + enrollmentId);
    }
}
