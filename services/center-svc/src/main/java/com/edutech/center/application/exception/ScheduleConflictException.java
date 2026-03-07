// src/main/java/com/edutech/center/application/exception/ScheduleConflictException.java
package com.edutech.center.application.exception;

public class ScheduleConflictException extends CenterException {
    public ScheduleConflictException(String room, String day) {
        super("Schedule conflict in room '" + room + "' on " + day);
    }
}
