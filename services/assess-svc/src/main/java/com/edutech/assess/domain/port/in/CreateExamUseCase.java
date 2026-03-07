// src/main/java/com/edutech/assess/domain/port/in/CreateExamUseCase.java
package com.edutech.assess.domain.port.in;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.CreateExamRequest;
import com.edutech.assess.application.dto.ExamResponse;

public interface CreateExamUseCase {
    ExamResponse createExam(CreateExamRequest request, AuthPrincipal principal);
}
