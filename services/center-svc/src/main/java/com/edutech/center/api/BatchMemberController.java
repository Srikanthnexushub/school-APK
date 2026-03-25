// src/main/java/com/edutech/center/api/BatchMemberController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AddBatchMemberRequest;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchMemberResponse;
import com.edutech.center.application.service.BatchMemberService;
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
@RequestMapping("/api/v1/centers/{centerId}/batches/{batchId}/members")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Batch Members", description = "Student roster management per batch")
public class BatchMemberController {

    private final BatchMemberService batchMemberService;

    public BatchMemberController(BatchMemberService batchMemberService) {
        this.batchMemberService = batchMemberService;
    }

    @GetMapping
    @Operation(summary = "List active students in a batch")
    public List<BatchMemberResponse> listMembers(@PathVariable UUID centerId,
                                                 @PathVariable UUID batchId,
                                                 @AuthenticationPrincipal AuthPrincipal principal) {
        return batchMemberService.listMembers(batchId, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a student to a batch (CENTER_ADMIN / INSTITUTION_ADMIN only)")
    public BatchMemberResponse addMember(@PathVariable UUID centerId,
                                         @PathVariable UUID batchId,
                                         @Valid @RequestBody AddBatchMemberRequest request,
                                         @AuthenticationPrincipal AuthPrincipal principal) {
        return batchMemberService.addMember(batchId, request, principal);
    }

    @DeleteMapping("/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Withdraw a student from a batch")
    public void withdrawMember(@PathVariable UUID centerId,
                               @PathVariable UUID batchId,
                               @PathVariable UUID studentId,
                               @AuthenticationPrincipal AuthPrincipal principal) {
        batchMemberService.withdrawMember(batchId, studentId, principal);
    }
}
