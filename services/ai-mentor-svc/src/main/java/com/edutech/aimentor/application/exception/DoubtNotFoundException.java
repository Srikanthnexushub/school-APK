package com.edutech.aimentor.application.exception;

import java.util.UUID;

public class DoubtNotFoundException extends AiMentorException {

    public DoubtNotFoundException(UUID doubtTicketId) {
        super("Doubt ticket not found: id=" + doubtTicketId);
    }

    public DoubtNotFoundException(UUID doubtTicketId, UUID studentId) {
        super("Doubt ticket not found: id=" + doubtTicketId + " studentId=" + studentId);
    }
}
