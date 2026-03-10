// src/main/java/com/edutech/assess/api/ExamController.java
package com.edutech.assess.api;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.CreateExamRequest;
import com.edutech.assess.application.dto.ExamResponse;
import com.edutech.assess.application.dto.StudentExamResponse;
import com.edutech.assess.domain.port.in.ListPublishedExamsUseCase;
import com.edutech.assess.application.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Exams")
public class ExamController {

    private final ExamService examService;
    private final ListPublishedExamsUseCase listPublishedExamsUseCase;

    public ExamController(ExamService examService, ListPublishedExamsUseCase listPublishedExamsUseCase) {
        this.examService = examService;
        this.listPublishedExamsUseCase = listPublishedExamsUseCase;
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
    @Operation(summary = "List exams. Teachers/admins get ExamResponse for their center; students get StudentExamResponse with enrollment status.")
    public ResponseEntity<Page<?>> listExams(
            @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false) UUID centerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (batchId != null) {
            return ResponseEntity.ok(examService.listByBatch(batchId, principal, PageRequest.of(page, size)));
        }
        if (principal.isTeacher() || principal.isCenterAdmin() || principal.isSuperAdmin()) {
            UUID effectiveCenterId = centerId != null ? centerId : principal.centerId();
            if (effectiveCenterId != null) {
                return ResponseEntity.ok(examService.listByCenter(effectiveCenterId, PageRequest.of(page, size)));
            }
        }
        return ResponseEntity.ok(examService.listPublishedExams(principal.userId(), PageRequest.of(page, size)));
    }

    @PutMapping("/{examId}/publish")
    public ExamResponse publishExam(
            @PathVariable UUID examId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return examService.publishExam(examId, principal);
    }
}
