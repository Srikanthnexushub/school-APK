package com.edutech.examtracker.domain.port.in;

import com.edutech.examtracker.application.dto.ExamEnrollmentResponse;

import java.util.List;
import java.util.UUID;

public interface GetEnrollmentUseCase {

    ExamEnrollmentResponse getEnrollment(UUID enrollmentId);

    List<ExamEnrollmentResponse> getStudentEnrollments(UUID studentId);
}
