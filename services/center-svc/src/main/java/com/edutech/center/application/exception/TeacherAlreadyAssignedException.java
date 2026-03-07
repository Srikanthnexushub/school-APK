// src/main/java/com/edutech/center/application/exception/TeacherAlreadyAssignedException.java
package com.edutech.center.application.exception;

import java.util.UUID;

public class TeacherAlreadyAssignedException extends CenterException {
    public TeacherAlreadyAssignedException(UUID userId, UUID centerId) {
        super("User " + userId + " is already a teacher at center " + centerId);
    }
}
