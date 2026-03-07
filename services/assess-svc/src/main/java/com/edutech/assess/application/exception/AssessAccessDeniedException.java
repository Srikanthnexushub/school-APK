// src/main/java/com/edutech/assess/application/exception/AssessAccessDeniedException.java
package com.edutech.assess.application.exception;

public class AssessAccessDeniedException extends AssessException {
    public AssessAccessDeniedException() {
        super("Access denied: insufficient permissions");
    }
}
