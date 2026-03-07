// src/main/java/com/edutech/assess/domain/port/in/AddQuestionUseCase.java
package com.edutech.assess.domain.port.in;

import com.edutech.assess.application.dto.AddQuestionRequest;
import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.QuestionResponse;

import java.util.UUID;

public interface AddQuestionUseCase {
    QuestionResponse addQuestion(UUID examId, AddQuestionRequest request, AuthPrincipal principal);
}
