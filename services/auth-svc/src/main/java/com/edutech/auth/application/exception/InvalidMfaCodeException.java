package com.edutech.auth.application.exception;

public class InvalidMfaCodeException extends AuthException {
    public InvalidMfaCodeException() {
        super("The authenticator code is invalid or has expired");
    }
}
