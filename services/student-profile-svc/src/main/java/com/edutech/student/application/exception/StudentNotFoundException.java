package com.edutech.student.application.exception;

import java.util.UUID;

public class StudentNotFoundException extends StudentPortalException {

    public StudentNotFoundException(UUID studentId) {
        super("Student not found: " + studentId);
    }
}
