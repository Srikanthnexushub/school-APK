// src/main/java/com/edutech/assess/application/exception/ExamNotFoundException.java
package com.edutech.assess.application.exception;

import java.util.UUID;

public class ExamNotFoundException extends AssessException {
    public ExamNotFoundException(UUID id) {
        super("Exam not found: " + id);
    }
}
