package com.edutech.aimentor.application.exception;

public class AiMentorException extends RuntimeException {

    public AiMentorException(String message) {
        super(message);
    }

    public AiMentorException(String message, Throwable cause) {
        super(message, cause);
    }
}
