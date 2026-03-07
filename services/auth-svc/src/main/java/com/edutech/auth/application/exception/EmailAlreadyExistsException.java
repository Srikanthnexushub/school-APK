// src/main/java/com/edutech/auth/application/exception/EmailAlreadyExistsException.java
package com.edutech.auth.application.exception;

public class EmailAlreadyExistsException extends AuthException {
    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
