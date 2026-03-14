package com.edutech.auth.application.exception;

public class MfaNotEnabledException extends AuthException {
    public MfaNotEnabledException() {
        super("Two-factor authentication is not enabled for this account");
    }
}
