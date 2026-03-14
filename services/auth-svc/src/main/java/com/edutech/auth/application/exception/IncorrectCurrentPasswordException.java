package com.edutech.auth.application.exception;

public class IncorrectCurrentPasswordException extends AuthException {
    public IncorrectCurrentPasswordException() {
        super("Current password is incorrect");
    }
}
