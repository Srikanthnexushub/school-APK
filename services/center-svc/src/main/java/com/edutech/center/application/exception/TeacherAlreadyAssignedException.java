// src/main/java/com/edutech/center/application/exception/TeacherAlreadyAssignedException.java
package com.edutech.center.application.exception;

import java.util.UUID;

public class TeacherAlreadyAssignedException extends CenterException {
    public TeacherAlreadyAssignedException(UUID userId, UUID centerId) {
        super("User " + userId + " is already a teacher at center " + centerId);
    }
    /** Used when detecting duplicate by email (staff invite flow). */
    public TeacherAlreadyAssignedException(String email, UUID centerId) {
        super("Staff member with email " + email + " already exists at center " + centerId);
    }
}
