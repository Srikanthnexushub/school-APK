// src/main/java/com/edutech/assess/domain/port/in/FindSimilarQuestionsUseCase.java
package com.edutech.assess.domain.port.in;

import com.edutech.assess.application.dto.SimilarQuestionRequest;
import com.edutech.assess.application.dto.SimilarQuestionResponse;

import java.util.List;

public interface FindSimilarQuestionsUseCase {
    List<SimilarQuestionResponse> findSimilarQuestions(SimilarQuestionRequest request);
}
