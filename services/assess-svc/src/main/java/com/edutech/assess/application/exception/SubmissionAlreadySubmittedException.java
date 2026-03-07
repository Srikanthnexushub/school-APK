// src/main/java/com/edutech/assess/application/exception/SubmissionAlreadySubmittedException.java
package com.edutech.assess.application.exception;

import java.util.UUID;

public class SubmissionAlreadySubmittedException extends AssessException {
    public SubmissionAlreadySubmittedException(UUID id) {
        super("Submission " + id + " has already been submitted");
    }
}
