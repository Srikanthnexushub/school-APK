package com.edutech.examtracker.application.exception;

import java.util.UUID;

public class EnrollmentNotFoundException extends ExamTrackerException {

    public EnrollmentNotFoundException(UUID id) {
        super("Enrollment not found: " + id);
    }
}
