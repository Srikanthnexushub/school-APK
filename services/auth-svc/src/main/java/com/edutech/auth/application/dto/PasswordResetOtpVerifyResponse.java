package com.edutech.auth.application.dto;

/**
 * Returned by POST /api/v1/otp/verify when purpose = PASSWORD_RESET.
 * The resetToken is short-lived (5 min TTL in Redis) and must be submitted
 * along with the new password to POST /api/v1/auth/reset-password.
 */
public record PasswordResetOtpVerifyResponse(String resetToken) {}
