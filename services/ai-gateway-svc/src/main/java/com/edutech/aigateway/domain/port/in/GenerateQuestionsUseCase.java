package com.edutech.aigateway.domain.port.in;

import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.domain.model.GeneratedQuestion;
import com.edutech.aigateway.domain.model.QuestionGenerationRequest;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GenerateQuestionsUseCase {
    Mono<List<GeneratedQuestion>> generateQuestions(QuestionGenerationRequest request, AuthPrincipal principal);
}
