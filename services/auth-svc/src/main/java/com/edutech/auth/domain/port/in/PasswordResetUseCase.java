package com.edutech.auth.domain.port.in;

public interface PasswordResetUseCase {
    /** Sends a PASSWORD_RESET OTP to the given email. No-op if email not found (prevents enumeration). */
    void sendPasswordResetOtp(String email);

    /** Verifies the OTP for password reset and returns a short-lived reset token. */
    String verifyPasswordResetOtp(String email, String otp);

    /** Completes the password reset using the token issued by verifyPasswordResetOtp. */
    void resetPassword(String email, String resetToken, String newPassword);
}
