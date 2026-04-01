package com.edutech.chat.application.service;

import java.util.UUID;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(UUID sessionId) {
        super("Chat session not found: " + sessionId);
    }
}
