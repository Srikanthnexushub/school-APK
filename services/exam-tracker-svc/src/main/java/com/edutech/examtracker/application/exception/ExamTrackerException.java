package com.edutech.examtracker.application.exception;

public abstract class ExamTrackerException extends RuntimeException {

    protected ExamTrackerException(String message) {
        super(message);
    }
}
