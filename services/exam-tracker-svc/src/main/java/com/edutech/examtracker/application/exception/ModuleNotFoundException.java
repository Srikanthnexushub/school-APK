package com.edutech.examtracker.application.exception;

import java.util.UUID;

public class ModuleNotFoundException extends ExamTrackerException {

    public ModuleNotFoundException(UUID id) {
        super("Syllabus module not found: " + id);
    }
}
