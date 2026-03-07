// src/main/java/com/edutech/auth/application/exception/OtpExpiredException.java
package com.edutech.auth.application.exception;

public class OtpExpiredException extends AuthException {
    public OtpExpiredException() { super("OTP has expired or does not exist"); }
}
