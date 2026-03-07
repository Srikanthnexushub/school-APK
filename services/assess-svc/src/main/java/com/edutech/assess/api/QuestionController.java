// src/main/java/com/edutech/assess/api/QuestionController.java
package com.edutech.assess.api;

import com.edutech.assess.application.dto.AddQuestionRequest;
import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.QuestionResponse;
import com.edutech.assess.application.service.QuestionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams/{examId}/questions")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse addQuestion(
            @PathVariable UUID examId,
            @Valid @RequestBody AddQuestionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return questionService.addQuestion(examId, request, principal);
    }

    @GetMapping
    public List<QuestionResponse> listQuestions(
            @PathVariable UUID examId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return questionService.listQuestions(examId, principal);
    }
}
