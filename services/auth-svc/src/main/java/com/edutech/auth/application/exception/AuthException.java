// src/main/java/com/edutech/auth/application/exception/AuthException.java
package com.edutech.auth.application.exception;

public abstract class AuthException extends RuntimeException {
    protected AuthException(String message) {
        super(message);
    }
    protected AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
