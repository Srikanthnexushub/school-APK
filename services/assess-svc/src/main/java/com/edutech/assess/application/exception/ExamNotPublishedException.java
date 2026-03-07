// src/main/java/com/edutech/assess/application/exception/ExamNotPublishedException.java
package com.edutech.assess.application.exception;

import java.util.UUID;

public class ExamNotPublishedException extends AssessException {
    public ExamNotPublishedException(UUID examId) {
        super("Exam " + examId + " is not published");
    }
}
