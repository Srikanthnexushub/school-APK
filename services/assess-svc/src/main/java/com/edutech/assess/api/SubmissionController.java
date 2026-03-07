// src/main/java/com/edutech/assess/api/SubmissionController.java
package com.edutech.assess.api;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.SubmissionResponse;
import com.edutech.assess.application.dto.SubmitAnswersRequest;
import com.edutech.assess.application.service.SubmissionService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams/{examId}/submissions")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse startSubmission(
            @PathVariable UUID examId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return submissionService.startSubmission(examId, principal);
    }

    @PostMapping("/{submissionId}/answers")
    public SubmissionResponse submitAnswers(
            @PathVariable UUID examId,
            @PathVariable UUID submissionId,
            @Valid @RequestBody SubmitAnswersRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return submissionService.submitAnswers(examId, submissionId, request, principal);
    }

    @GetMapping("/{submissionId}")
    public SubmissionResponse getSubmission(
            @PathVariable UUID examId,
            @PathVariable UUID submissionId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return submissionService.getSubmission(submissionId, principal);
    }
}
