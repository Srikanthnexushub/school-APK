// src/main/java/com/edutech/assess/application/exception/AssignmentAccessDeniedException.java
package com.edutech.assess.application.exception;

public class AssignmentAccessDeniedException extends AssessException {
    public AssignmentAccessDeniedException() {
        super("Access denied to assignment resource");
    }
}
