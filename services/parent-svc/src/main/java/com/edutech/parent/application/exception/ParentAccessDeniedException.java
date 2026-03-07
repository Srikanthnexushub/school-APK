// src/main/java/com/edutech/parent/application/exception/ParentAccessDeniedException.java
package com.edutech.parent.application.exception;

public class ParentAccessDeniedException extends ParentException {
    public ParentAccessDeniedException() {
        super("Access denied: you do not have permission to perform this action");
    }
}
