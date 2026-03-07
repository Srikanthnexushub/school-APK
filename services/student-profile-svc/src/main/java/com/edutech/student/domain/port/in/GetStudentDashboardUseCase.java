package com.edutech.student.domain.port.in;

import com.edutech.student.application.dto.StudentDashboardResponse;

import java.util.UUID;

public interface GetStudentDashboardUseCase {
    StudentDashboardResponse getDashboard(UUID studentId);
}
