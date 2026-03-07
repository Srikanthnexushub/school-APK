package com.edutech.aimentor.application.exception;

import java.util.UUID;

public class StudyPlanNotFoundException extends AiMentorException {

    public StudyPlanNotFoundException(UUID studentId, UUID enrollmentId) {
        super("Study plan not found for studentId=" + studentId + " enrollmentId=" + enrollmentId);
    }

    public StudyPlanNotFoundException(UUID id) {
        super("Study plan not found: id=" + id);
    }
}
