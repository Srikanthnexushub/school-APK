// src/main/java/com/edutech/parent/application/exception/DuplicateStudentLinkException.java
package com.edutech.parent.application.exception;

import java.util.UUID;

public class DuplicateStudentLinkException extends ParentException {
    public DuplicateStudentLinkException(UUID studentId) {
        super("Student " + studentId + " is already linked to this parent");
    }
}
