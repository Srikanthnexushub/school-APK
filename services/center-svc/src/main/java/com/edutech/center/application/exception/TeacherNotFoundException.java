// src/main/java/com/edutech/center/application/exception/TeacherNotFoundException.java
package com.edutech.center.application.exception;

import java.util.UUID;

public class TeacherNotFoundException extends CenterException {
    public TeacherNotFoundException(UUID teacherId) {
        super("Teacher not found: " + teacherId);
    }
}
