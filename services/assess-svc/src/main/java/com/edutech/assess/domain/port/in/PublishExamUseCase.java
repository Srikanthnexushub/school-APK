// src/main/java/com/edutech/assess/domain/port/in/PublishExamUseCase.java
package com.edutech.assess.domain.port.in;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.ExamResponse;

import java.util.UUID;

public interface PublishExamUseCase {
    ExamResponse publishExam(UUID examId, AuthPrincipal principal);
}
