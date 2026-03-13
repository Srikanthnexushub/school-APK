package com.edutech.auth.application.exception;

public class OtpMaxResendsExceededException extends AuthException {
    public OtpMaxResendsExceededException() {
        super("Maximum OTP resend limit reached. Please wait before requesting a new code.");
    }
}
