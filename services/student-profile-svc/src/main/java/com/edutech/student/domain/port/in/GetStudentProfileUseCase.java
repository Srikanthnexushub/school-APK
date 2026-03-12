package com.edutech.student.domain.port.in;

import com.edutech.student.application.dto.StudentProfileResponse;

import java.util.UUID;

public interface GetStudentProfileUseCase {
    StudentProfileResponse getProfile(UUID studentId);
    StudentProfileResponse getProfileByUserId(UUID userId);
}
