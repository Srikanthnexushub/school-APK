// src/main/java/com/edutech/center/application/exception/DuplicateCenterCodeException.java
package com.edutech.center.application.exception;

public class DuplicateCenterCodeException extends CenterException {
    public DuplicateCenterCodeException(String code) {
        super("Center code already exists: " + code);
    }
}
