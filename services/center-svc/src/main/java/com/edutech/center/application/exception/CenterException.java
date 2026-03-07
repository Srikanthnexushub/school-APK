// src/main/java/com/edutech/center/application/exception/CenterException.java
package com.edutech.center.application.exception;

public abstract class CenterException extends RuntimeException {
    protected CenterException(String message) { super(message); }
}
