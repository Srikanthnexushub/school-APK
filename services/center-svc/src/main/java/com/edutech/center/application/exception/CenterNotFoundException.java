// src/main/java/com/edutech/center/application/exception/CenterNotFoundException.java
package com.edutech.center.application.exception;

import java.util.UUID;

public class CenterNotFoundException extends CenterException {
    public CenterNotFoundException(UUID centerId) {
        super("Center not found: " + centerId);
    }
}
