// src/main/java/com/edutech/auth/application/exception/OtpMaxAttemptsExceededException.java
package com.edutech.auth.application.exception;

public class OtpMaxAttemptsExceededException extends AuthException {
    public OtpMaxAttemptsExceededException() {
        super("Maximum OTP verification attempts exceeded");
    }
}
