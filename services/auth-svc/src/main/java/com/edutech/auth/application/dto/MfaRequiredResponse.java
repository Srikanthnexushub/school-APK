package com.edutech.auth.application.dto;

/**
 * Returned as HTTP 202 when the user's password is correct but MFA is enabled.
 * The client must POST pendingMfaToken + totpCode to /api/v1/auth/mfa/verify
 * to obtain the full token pair.
 */
public record MfaRequiredResponse(
    boolean mfaRequired,
    String pendingMfaToken
) {
    public MfaRequiredResponse(String pendingMfaToken) {
        this(true, pendingMfaToken);
    }
}
