// src/main/java/com/edutech/center/api/TeacherController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AcceptInvitationRequest;
import com.edutech.center.application.dto.AssignTeacherRequest;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BulkImportConfirmResponse;
import com.edutech.center.application.dto.BulkImportPreviewResponse;
import com.edutech.center.application.dto.InvitationLookupResponse;
import com.edutech.center.application.dto.RejectTeacherRequest;
import com.edutech.center.application.dto.TeacherResponse;
import com.edutech.center.application.dto.TeacherSelfRegisterRequest;
import com.edutech.center.application.service.TeacherApprovalService;
import com.edutech.center.application.service.TeacherBulkImportService;
import com.edutech.center.application.service.TeacherService;
import com.edutech.center.domain.model.SubjectCatalog;
import com.edutech.center.domain.model.Teacher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/centers/{centerId}/teachers")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Teachers", description = "Teacher assignment and onboarding within a center")
public class TeacherController {

    private final TeacherService teacherService;
    private final TeacherBulkImportService bulkImportService;
    private final TeacherApprovalService approvalService;

    public TeacherController(TeacherService teacherService,
                              TeacherBulkImportService bulkImportService,
                              TeacherApprovalService approvalService) {
        this.teacherService = teacherService;
        this.bulkImportService = bulkImportService;
        this.approvalService = approvalService;
    }

    // ─── Existing ─────────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign an existing user as teacher to this center")
    public TeacherResponse assignTeacher(@PathVariable UUID centerId,
                                         @Valid @RequestBody AssignTeacherRequest request,
                                         @AuthenticationPrincipal AuthPrincipal principal) {
        return teacherService.assignTeacher(centerId, request, principal);
    }

    @GetMapping
    @Operation(summary = "List teachers for a center")
    public Page<TeacherResponse> listTeachers(@PathVariable UUID centerId,
                                              @AuthenticationPrincipal AuthPrincipal principal,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        return teacherService.listTeachers(centerId, principal, PageRequest.of(page, size));
    }

    // ─── Bulk Import ──────────────────────────────────────────────────────────

    @GetMapping("/bulk-template")
    @Operation(summary = "Download the CSV template for bulk teacher import")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable UUID centerId) {
        String header = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n";
        String sample = "Rajesh,Kumar,rajesh.kumar@school.edu.in,+919876543210,\"Mathematics,Physics\",T-042\n";
        String subjects = "# Valid subjects: " + String.join(", ", SubjectCatalog.SUBJECTS) + "\n";
        byte[] content = (subjects + header + sample).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "teacher-import-template.csv");
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @PostMapping(value = "/bulk-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Validate a teacher CSV and return a preview with errors")
    public BulkImportPreviewResponse bulkPreview(@PathVariable UUID centerId,
                                                  @RequestParam("file") MultipartFile file,
                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return bulkImportService.preview(centerId, file, principal);
    }

    @PostMapping(value = "/bulk-confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Import valid rows from a teacher CSV and send invitation emails")
    public BulkImportConfirmResponse bulkConfirm(@PathVariable UUID centerId,
                                                  @RequestParam("file") MultipartFile file,
                                                  @RequestParam(defaultValue = "false") boolean skipErrors,
                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return bulkImportService.confirm(centerId, file, skipErrors, principal);
    }

    // ─── Self-Registration & Approval ─────────────────────────────────────────

    @PostMapping("/self-register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Teacher self-registers to this center — creates PENDING_APPROVAL record")
    public TeacherResponse selfRegister(@PathVariable UUID centerId,
                                        @Valid @RequestBody TeacherSelfRegisterRequest request,
                                        @AuthenticationPrincipal AuthPrincipal principal) {
        return approvalService.selfRegister(centerId, request, principal);
    }

    @GetMapping("/pending")
    @Operation(summary = "List self-registered teachers awaiting approval")
    public List<TeacherResponse> listPending(@PathVariable UUID centerId,
                                             @AuthenticationPrincipal AuthPrincipal principal) {
        return approvalService.listPending(centerId, principal);
    }

    @PostMapping("/{teacherId}/approve")
    @Operation(summary = "Approve a self-registered teacher")
    public TeacherResponse approve(@PathVariable UUID centerId,
                                   @PathVariable UUID teacherId,
                                   @AuthenticationPrincipal AuthPrincipal principal) {
        return approvalService.approve(centerId, teacherId, principal);
    }

    @PostMapping("/{teacherId}/reject")
    @Operation(summary = "Reject a self-registered teacher")
    public TeacherResponse reject(@PathVariable UUID centerId,
                                  @PathVariable UUID teacherId,
                                  @Valid @RequestBody RejectTeacherRequest request,
                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return approvalService.reject(centerId, teacherId, principal);
    }

    // ─── Invitation Accept ────────────────────────────────────────────────────

    @PostMapping("/accept-invitation")
    @Operation(summary = "Link a bulk-imported teacher stub to the newly registered userId")
    public ResponseEntity<Void> acceptInvitation(@PathVariable UUID centerId,
                                                  @Valid @RequestBody AcceptInvitationRequest request) {
        bulkImportService.acceptInvitation(request.token(), request.userId());
        return ResponseEntity.ok().build();
    }
}
