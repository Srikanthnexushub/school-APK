// src/main/java/com/edutech/assess/domain/port/in/StartSubmissionUseCase.java
package com.edutech.assess.domain.port.in;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.SubmissionResponse;

import java.util.UUID;

public interface StartSubmissionUseCase {
    SubmissionResponse startSubmission(UUID examId, AuthPrincipal principal);
}
