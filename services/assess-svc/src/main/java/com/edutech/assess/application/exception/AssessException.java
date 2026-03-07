// src/main/java/com/edutech/assess/application/exception/AssessException.java
package com.edutech.assess.application.exception;

public abstract class AssessException extends RuntimeException {
    protected AssessException(String msg) {
        super(msg);
    }
}
