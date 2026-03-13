package com.edutech.student.application.exception;

public class InvalidLinkOtpException extends RuntimeException {
    public InvalidLinkOtpException() {
        super("Invalid or expired OTP. Please ask the parent to generate a new one.");
    }
}
