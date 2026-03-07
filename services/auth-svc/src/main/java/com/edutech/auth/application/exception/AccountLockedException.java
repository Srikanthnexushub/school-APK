// src/main/java/com/edutech/auth/application/exception/AccountLockedException.java
package com.edutech.auth.application.exception;

public class AccountLockedException extends AuthException {
    public AccountLockedException() { super("Account is locked"); }
}
