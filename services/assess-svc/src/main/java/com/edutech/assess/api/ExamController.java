// src/main/java/com/edutech/assess/api/ExamController.java
package com.edutech.assess.api;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.CreateExamRequest;
import com.edutech.assess.application.dto.ExamResponse;
import com.edutech.assess.application.service.ExamService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResponse createExam(
            @Valid @RequestBody CreateExamRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return examService.createExam(request, principal);
    }

    @GetMapping("/{examId}")
    public ExamResponse getExam(
            @PathVariable UUID examId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return examService.getExam(examId, principal);
    }

    @GetMapping
    public List<ExamResponse> listByBatch(
            @RequestParam UUID batchId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return examService.listByBatch(batchId, principal);
    }

    @PutMapping("/{examId}/publish")
    public ExamResponse publishExam(
            @PathVariable UUID examId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return examService.publishExam(examId, principal);
    }
}
