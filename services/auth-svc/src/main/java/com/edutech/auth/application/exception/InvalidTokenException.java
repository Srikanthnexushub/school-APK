// src/main/java/com/edutech/auth/application/exception/InvalidTokenException.java
package com.edutech.auth.application.exception;

public class InvalidTokenException extends AuthException {
    public InvalidTokenException(String message) { super(message); }
}
