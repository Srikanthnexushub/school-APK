package com.edutech.auth.application.exception;

/**
 * Thrown by AuthenticationService when the user has MFA enabled.
 * Carries a short-lived pending token the client uses to complete the MFA step.
 * GlobalExceptionHandler maps this to HTTP 202 Accepted.
 */
public class MfaRequiredException extends RuntimeException {

    private final String pendingMfaToken;

    public MfaRequiredException(String pendingMfaToken) {
        super("Two-factor authentication required");
        this.pendingMfaToken = pendingMfaToken;
    }

    public String getPendingMfaToken() {
        return pendingMfaToken;
    }
}
