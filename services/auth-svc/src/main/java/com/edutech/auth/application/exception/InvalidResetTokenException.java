package com.edutech.auth.application.exception;

public class InvalidResetTokenException extends AuthException {
    public InvalidResetTokenException() {
        super("Password reset token is invalid or has expired");
    }
}
