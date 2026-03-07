// src/main/java/com/edutech/auth/application/exception/AccountNotVerifiedException.java
package com.edutech.auth.application.exception;

public class AccountNotVerifiedException extends AuthException {
    public AccountNotVerifiedException() {
        super("Account is not yet verified. Please verify your email.");
    }
}
