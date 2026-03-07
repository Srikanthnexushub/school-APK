package com.edutech.student.domain.port.in;

import com.edutech.student.application.dto.CreateStudentProfileRequest;
import com.edutech.student.application.dto.StudentProfileResponse;

public interface CreateStudentProfileUseCase {
    StudentProfileResponse createProfile(CreateStudentProfileRequest request);
}
