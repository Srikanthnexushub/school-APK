package com.edutech.auth.application.exception;

public class MfaAlreadyEnabledException extends AuthException {
    public MfaAlreadyEnabledException() {
        super("Two-factor authentication is already enabled for this account");
    }
}
