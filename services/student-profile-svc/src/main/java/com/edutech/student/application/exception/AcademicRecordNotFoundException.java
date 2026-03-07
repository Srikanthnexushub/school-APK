package com.edutech.student.application.exception;

import java.util.UUID;

public class AcademicRecordNotFoundException extends StudentPortalException {

    public AcademicRecordNotFoundException(UUID id) {
        super("Academic record not found: " + id);
    }
}
