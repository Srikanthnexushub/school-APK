package com.edutech.mentorsvc.application.exception;

import java.util.UUID;

public class MentorSessionNotFoundException extends MentorSvcException {

    public MentorSessionNotFoundException(UUID sessionId) {
        super("Mentor session not found with id: " + sessionId, "SESSION_NOT_FOUND");
    }

    public MentorSessionNotFoundException(String message) {
        super(message, "SESSION_NOT_FOUND");
    }
}
