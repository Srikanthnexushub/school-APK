package com.edutech.student.domain.port.in;

import com.edutech.student.application.dto.StudentProfileResponse;
import com.edutech.student.application.dto.UpdateStudentProfileRequest;

import java.util.UUID;

public interface UpdateStudentProfileUseCase {
    StudentProfileResponse updateProfile(UUID studentId, UpdateStudentProfileRequest request);
}
