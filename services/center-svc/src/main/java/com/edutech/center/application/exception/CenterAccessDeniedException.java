// src/main/java/com/edutech/center/application/exception/CenterAccessDeniedException.java
package com.edutech.center.application.exception;

public class CenterAccessDeniedException extends CenterException {
    public CenterAccessDeniedException() {
        super("You do not have permission to access this center's resources");
    }
}
