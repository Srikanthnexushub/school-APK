package com.edutech.student.application.exception;

public abstract class StudentPortalException extends RuntimeException {

    protected StudentPortalException(String message) {
        super(message);
    }

    protected StudentPortalException(String message, Throwable cause) {
        super(message, cause);
    }
}
