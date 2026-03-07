// src/main/java/com/edutech/auth/application/exception/CaptchaVerificationException.java
package com.edutech.auth.application.exception;

public class CaptchaVerificationException extends AuthException {
    public CaptchaVerificationException(String message) { super(message); }
}
