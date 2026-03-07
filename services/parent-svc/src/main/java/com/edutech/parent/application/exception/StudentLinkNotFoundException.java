// src/main/java/com/edutech/parent/application/exception/StudentLinkNotFoundException.java
package com.edutech.parent.application.exception;

import java.util.UUID;

public class StudentLinkNotFoundException extends ParentException {
    public StudentLinkNotFoundException(UUID id) {
        super("Student link not found: " + id);
    }
}
