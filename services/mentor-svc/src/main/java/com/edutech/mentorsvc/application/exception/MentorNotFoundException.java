package com.edutech.mentorsvc.application.exception;

import java.util.UUID;

public class MentorNotFoundException extends MentorSvcException {

    public MentorNotFoundException(UUID mentorId) {
        super("Mentor not found with id: " + mentorId, "MENTOR_NOT_FOUND");
    }

    public MentorNotFoundException(String message) {
        super(message, "MENTOR_NOT_FOUND");
    }
}
