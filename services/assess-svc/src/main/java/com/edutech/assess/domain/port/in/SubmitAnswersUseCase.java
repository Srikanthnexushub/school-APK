// src/main/java/com/edutech/assess/domain/port/in/SubmitAnswersUseCase.java
package com.edutech.assess.domain.port.in;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.SubmissionResponse;
import com.edutech.assess.application.dto.SubmitAnswersRequest;

import java.util.UUID;

public interface SubmitAnswersUseCase {
    SubmissionResponse submitAnswers(UUID examId, UUID submissionId, SubmitAnswersRequest request, AuthPrincipal principal);
}
