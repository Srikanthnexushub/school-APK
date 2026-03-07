// src/main/java/com/edutech/parent/api/StudentLinkController.java
package com.edutech.parent.api;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.LinkStudentRequest;
import com.edutech.parent.application.dto.StudentLinkResponse;
import com.edutech.parent.application.service.StudentLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/parents/{profileId}/students")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Student Links", description = "Parent-student relationship management")
public class StudentLinkController {

    private final StudentLinkService studentLinkService;

    public StudentLinkController(StudentLinkService studentLinkService) {
        this.studentLinkService = studentLinkService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Link a student to this parent profile")
    public StudentLinkResponse linkStudent(
            @PathVariable UUID profileId,
            @Valid @RequestBody LinkStudentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return studentLinkService.linkStudent(profileId, request, principal);
    }

    @GetMapping
    @Operation(summary = "List all active student links for this parent")
    public List<StudentLinkResponse> listLinkedStudents(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return studentLinkService.listLinkedStudents(profileId, principal);
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a student link")
    public void revokeLink(
            @PathVariable UUID profileId,
            @PathVariable UUID linkId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        studentLinkService.revokeLink(linkId, principal);
    }
}
