package com.edutech.examtracker.application.exception;

import com.edutech.examtracker.domain.model.ExamCode;

import java.util.UUID;

public class DuplicateEnrollmentException extends ExamTrackerException {

    public DuplicateEnrollmentException(UUID studentId, ExamCode examCode) {
        super("Student " + studentId + " is already enrolled in exam: " + examCode);
    }
}
