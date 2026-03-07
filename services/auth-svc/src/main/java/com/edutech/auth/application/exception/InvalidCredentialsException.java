// src/main/java/com/edutech/auth/application/exception/InvalidCredentialsException.java
package com.edutech.auth.application.exception;

public class InvalidCredentialsException extends AuthException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
