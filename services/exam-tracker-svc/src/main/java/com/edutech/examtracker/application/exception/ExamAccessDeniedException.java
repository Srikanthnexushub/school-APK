package com.edutech.examtracker.application.exception;

/**
 * Thrown when the authenticated principal attempts to access another user's exam-tracker data.
 */
public class ExamAccessDeniedException extends ExamTrackerException {

    public ExamAccessDeniedException(String message) {
        super(message);
    }
}
