package com.edutech.psych.application.exception;

import java.util.UUID;

public class SessionHistoryNotFoundException extends PsychException {

    public SessionHistoryNotFoundException(UUID id) {
        super("Session not found: " + id);
    }
}
