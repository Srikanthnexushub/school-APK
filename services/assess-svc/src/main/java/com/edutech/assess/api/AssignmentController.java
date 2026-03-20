// src/main/java/com/edutech/assess/api/AssignmentController.java
package com.edutech.assess.api;

import com.edutech.assess.application.dto.AssignmentResponse;
import com.edutech.assess.application.dto.AssignmentSubmissionResponse;
import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.CreateAssignmentRequest;
import com.edutech.assess.application.dto.GradeSubmissionRequest;
import com.edutech.assess.application.dto.SubmitAssignmentRequest;
import com.edutech.assess.application.dto.UpdateAssignmentRequest;
import com.edutech.assess.application.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new assignment")
    public AssignmentResponse createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return assignmentService.createAssignment(request, principal);
    }

    @GetMapping("/assignments/{id}")
    @Operation(summary = "Get an assignment by ID")
    public AssignmentResponse getAssignment(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return assignmentService.getAssignment(id, principal);
    }

    @GetMapping("/assignments")
    @Operation(summary = "List assignments by batchId or centerId")
    public ResponseEntity<List<AssignmentResponse>> listAssignments(
            @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false) UUID centerId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (batchId != null) {
            return ResponseEntity.ok(assignmentService.listByBatch(batchId, principal));
        }
        if (centerId != null) {
            return ResponseEntity.ok(assignmentService.listByCenter(centerId, principal));
        }
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/assignments/{id}")
    @Operation(summary = "Update an assignment (DRAFT only)")
    public AssignmentResponse updateAssignment(
            @PathVariable UUID id,
            @RequestBody UpdateAssignmentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return assignmentService.updateAssignment(id, request, principal);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete an assignment")
    public void deleteAssignment(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        assignmentService.deleteAssignment(id, principal);
    }

    @PatchMapping("/assignments/{id}/publish")
    @Operation(summary = "Publish an assignment")
    public AssignmentResponse publishAssignment(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return assignmentService.publishAssignment(id, principal);
    }

    @PatchMapping("/assignments/{id}/close")
    @Operation(summary = "Close a published assignment")
    public AssignmentResponse closeAssignment(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return assignmentService.closeAssignment(id, principal);
    }

    @PostMapping("/assignments/{id}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit an assignment (students only)")
    public AssignmentSubmissionResponse submitAssignment(
            @PathVariable UUID id,
            @RequestBody SubmitAssignmentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return assignmentService.submitAssignment(id, request, principal);
    }

    @GetMapping("/assignments/{id}/submissions")
    @Operation(summary = "List submissions for an assignment")
    public List<AssignmentSubmissionResponse> listSubmissions(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return assignmentService.listSubmissions(id, principal);
    }

    @PatchMapping("/assignments/{id}/submissions/{subId}/grade")
    @Operation(summary = "Grade a submission")
    public AssignmentSubmissionResponse gradeSubmission(
            @PathVariable UUID id,
            @PathVariable UUID subId,
            @Valid @RequestBody GradeSubmissionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return assignmentService.gradeSubmission(id, subId, request, principal);
    }

    @GetMapping("/students/{studentId}/assignments")
    @Operation(summary = "List assignment submissions for a student")
    public List<AssignmentSubmissionResponse> listStudentSubmissions(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return assignmentService.listStudentSubmissions(studentId, principal);
    }
}
