// src/main/java/com/edutech/center/api/BatchFeeController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AssignFeeRequest;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchFeeAssignmentResponse;
import com.edutech.center.application.service.BatchFeeService;
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
@RequestMapping("/api/v1/centers/{centerId}/batches/{batchId}/fees")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Batch Fee Assignments", description = "Link fee structures to batches")
public class BatchFeeController {

    private final BatchFeeService batchFeeService;

    public BatchFeeController(BatchFeeService batchFeeService) {
        this.batchFeeService = batchFeeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign a fee structure to a batch (CENTER_ADMIN only)")
    public BatchFeeAssignmentResponse assign(@PathVariable UUID centerId,
                                             @PathVariable UUID batchId,
                                             @Valid @RequestBody AssignFeeRequest request,
                                             @AuthenticationPrincipal AuthPrincipal principal) {
        return batchFeeService.assign(batchId, request, principal);
    }

    @GetMapping
    @Operation(summary = "List fee structures assigned to this batch")
    public List<BatchFeeAssignmentResponse> listByBatch(@PathVariable UUID centerId,
                                                         @PathVariable UUID batchId,
                                                         @AuthenticationPrincipal AuthPrincipal principal) {
        return batchFeeService.listByBatch(batchId, principal);
    }

    @DeleteMapping("/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a fee assignment from a batch")
    public void remove(@PathVariable UUID centerId,
                       @PathVariable UUID batchId,
                       @PathVariable UUID assignmentId,
                       @AuthenticationPrincipal AuthPrincipal principal) {
        batchFeeService.remove(batchId, assignmentId, principal);
    }
}
