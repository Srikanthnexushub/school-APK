// src/main/java/com/edutech/center/application/exception/RegistrationNotFoundException.java
package com.edutech.center.application.exception;

public class RegistrationNotFoundException extends CenterException {
    public RegistrationNotFoundException() {
        super("No institution registration found for this account");
    }
}
