package com.edutech.parent.application.exception;

public class ConversationNotFoundException extends ParentException {
    public ConversationNotFoundException(Long conversationId) {
        super("Copilot conversation not found: " + conversationId);
    }
}
