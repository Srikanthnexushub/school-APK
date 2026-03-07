// src/main/java/com/edutech/auth/application/exception/UserNotFoundException.java
package com.edutech.auth.application.exception;

public class UserNotFoundException extends AuthException {
    public UserNotFoundException(String identifier) {
        super("User not found: " + identifier);
    }
}
