// src/main/java/com/edutech/center/application/exception/InstitutionAlreadyRegisteredException.java
package com.edutech.center.application.exception;

public class InstitutionAlreadyRegisteredException extends CenterException {
    public InstitutionAlreadyRegisteredException() {
        super("You already have a pending or active institution registration");
    }
}
