// src/main/java/com/edutech/assess/application/exception/SubmissionNotFoundException.java
package com.edutech.assess.application.exception;

import java.util.UUID;

public class SubmissionNotFoundException extends AssessException {
    public SubmissionNotFoundException(UUID id) {
        super("Submission not found: " + id);
    }
}
