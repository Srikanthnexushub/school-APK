// src/main/java/com/edutech/assess/api/EnrollmentController.java
package com.edutech.assess.api;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.EnrollStudentRequest;
import com.edutech.assess.application.dto.EnrollmentResponse;
import com.edutech.assess.application.service.EnrollmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams/{examId}/enrollments")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enrollStudent(
            @PathVariable UUID examId,
            @RequestBody(required = false) EnrollStudentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        // Always use the authenticated user's ID — ignore any studentId in the body
        EnrollStudentRequest secureRequest = new EnrollStudentRequest(principal.userId());
        return enrollmentService.enrollStudent(examId, secureRequest, principal);
    }

    @GetMapping
    public Page<EnrollmentResponse> listEnrollments(
            @PathVariable UUID examId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return enrollmentService.listEnrollments(examId, principal, PageRequest.of(page, size));
    }
}
