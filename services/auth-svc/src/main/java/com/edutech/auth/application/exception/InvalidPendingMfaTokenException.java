package com.edutech.auth.application.exception;

public class InvalidPendingMfaTokenException extends AuthException {
    public InvalidPendingMfaTokenException() {
        super("MFA session token is invalid or has expired — please log in again");
    }
}
