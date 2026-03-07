// src/main/java/com/edutech/assess/application/exception/QuestionNotFoundException.java
package com.edutech.assess.application.exception;

import java.util.UUID;

public class QuestionNotFoundException extends AssessException {
    public QuestionNotFoundException(UUID id) {
        super("Question not found: " + id);
    }
}
