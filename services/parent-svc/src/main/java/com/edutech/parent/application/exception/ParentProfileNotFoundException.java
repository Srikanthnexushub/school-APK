// src/main/java/com/edutech/parent/application/exception/ParentProfileNotFoundException.java
package com.edutech.parent.application.exception;

import java.util.UUID;

public class ParentProfileNotFoundException extends ParentException {
    public ParentProfileNotFoundException(UUID id) {
        super("Parent profile not found" + (id != null ? ": " + id : ""));
    }
}
