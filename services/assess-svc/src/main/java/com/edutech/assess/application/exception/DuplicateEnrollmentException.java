// src/main/java/com/edutech/assess/application/exception/DuplicateEnrollmentException.java
package com.edutech.assess.application.exception;

import java.util.UUID;

public class DuplicateEnrollmentException extends AssessException {
    public DuplicateEnrollmentException(UUID studentId, UUID examId) {
        super("Student " + studentId + " is already enrolled in exam " + examId);
    }
}
