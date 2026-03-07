package com.edutech.performance.application.exception;

public abstract class PerformanceException extends RuntimeException {

    protected PerformanceException(String message) {
        super(message);
    }

    protected PerformanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
