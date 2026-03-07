package com.edutech.mentorsvc.application.exception;

import java.util.UUID;

public class SessionAlreadyBookedException extends MentorSvcException {

    public SessionAlreadyBookedException(UUID sessionId) {
        super("Session already booked or feedback already submitted for session: " + sessionId,
                "SESSION_ALREADY_BOOKED");
    }

    public SessionAlreadyBookedException(String message) {
        super(message, "SESSION_ALREADY_BOOKED");
    }
}
