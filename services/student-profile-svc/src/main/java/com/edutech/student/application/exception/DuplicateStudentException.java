package com.edutech.student.application.exception;

public class DuplicateStudentException extends StudentPortalException {

    public DuplicateStudentException(String email) {
        super("Student already exists: " + email);
    }
}
