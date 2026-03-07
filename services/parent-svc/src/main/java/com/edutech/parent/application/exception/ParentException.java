// src/main/java/com/edutech/parent/application/exception/ParentException.java
package com.edutech.parent.application.exception;

public abstract class ParentException extends RuntimeException {
    protected ParentException(String message) {
        super(message);
    }
}
