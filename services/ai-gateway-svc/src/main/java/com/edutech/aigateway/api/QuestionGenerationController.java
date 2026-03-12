package com.edutech.aigateway.api;

import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.domain.model.GeneratedQuestion;
import com.edutech.aigateway.domain.model.QuestionGenerationRequest;
import com.edutech.aigateway.domain.port.in.GenerateQuestionsUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
public class QuestionGenerationController {

    private final GenerateQuestionsUseCase generateQuestionsUseCase;

    public QuestionGenerationController(GenerateQuestionsUseCase generateQuestionsUseCase) {
        this.generateQuestionsUseCase = generateQuestionsUseCase;
    }

    @PostMapping("/generate-questions")
    @ResponseStatus(HttpStatus.OK)
    public Mono<List<GeneratedQuestion>> generateQuestions(
            @Valid @RequestBody QuestionGenerationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return generateQuestionsUseCase.generateQuestions(request, principal);
    }
}
