package com.edutech.examtracker.domain.port.in;

import com.edutech.examtracker.application.dto.EnrollInExamRequest;
import com.edutech.examtracker.application.dto.ExamEnrollmentResponse;

import java.util.UUID;

public interface EnrollInExamUseCase {

    ExamEnrollmentResponse enroll(UUID studentId, EnrollInExamRequest request);
}
