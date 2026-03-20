// src/main/java/com/edutech/assess/application/exception/AssignmentNotFoundException.java
package com.edutech.assess.application.exception;

import java.util.UUID;

public class AssignmentNotFoundException extends AssessException {
    public AssignmentNotFoundException(UUID id) {
        super("Assignment not found: " + id);
    }
}
