// src/main/java/com/edutech/assess/application/exception/MaxAttemptsExceededException.java
package com.edutech.assess.application.exception;

import java.util.UUID;

public class MaxAttemptsExceededException extends AssessException {
    public MaxAttemptsExceededException(UUID examId, int maxAttempts) {
        super("Maximum attempts (" + maxAttempts + ") exceeded for exam " + examId);
    }
}
