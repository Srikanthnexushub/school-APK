package com.edutech.psych.application.exception;

import java.util.UUID;

public class SessionAlreadyCompletedException extends PsychException {

    public SessionAlreadyCompletedException(UUID id) {
        super("Session already completed: " + id);
    }
}
