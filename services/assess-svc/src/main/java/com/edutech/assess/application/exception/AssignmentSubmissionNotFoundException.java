// src/main/java/com/edutech/assess/application/exception/AssignmentSubmissionNotFoundException.java
package com.edutech.assess.application.exception;

import java.util.UUID;

public class AssignmentSubmissionNotFoundException extends AssessException {
    public AssignmentSubmissionNotFoundException(UUID id) {
        super("Assignment submission not found: " + id);
    }
}
