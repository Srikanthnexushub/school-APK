// src/main/java/com/edutech/assess/application/exception/EnrollmentNotFoundException.java
package com.edutech.assess.application.exception;

import java.util.UUID;

public class EnrollmentNotFoundException extends AssessException {
    public EnrollmentNotFoundException(UUID studentId, UUID examId) {
        super("No enrollment found for student " + studentId + " in exam " + examId);
    }
}
