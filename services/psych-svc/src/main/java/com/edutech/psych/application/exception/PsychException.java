package com.edutech.psych.application.exception;

public abstract class PsychException extends RuntimeException {

    protected PsychException(String message) {
        super(message);
    }
}
