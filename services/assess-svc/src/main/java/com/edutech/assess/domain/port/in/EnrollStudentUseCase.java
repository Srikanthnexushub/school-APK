// src/main/java/com/edutech/assess/domain/port/in/EnrollStudentUseCase.java
package com.edutech.assess.domain.port.in;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.EnrollStudentRequest;
import com.edutech.assess.application.dto.EnrollmentResponse;

import java.util.UUID;

public interface EnrollStudentUseCase {
    EnrollmentResponse enrollStudent(UUID examId, EnrollStudentRequest request, AuthPrincipal principal);
}
