package com.edutech.careeroracle.application.exception;

/**
 * Thrown when the authenticated principal attempts to access another user's career data.
 */
public class CareerAccessDeniedException extends CareerOracleException {

    public CareerAccessDeniedException(String message) {
        super(message);
    }
}
