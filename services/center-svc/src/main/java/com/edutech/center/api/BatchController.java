// src/main/java/com/edutech/center/api/BatchController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchResponse;
import com.edutech.center.application.dto.CreateBatchRequest;
import com.edutech.center.application.dto.UpdateBatchRequest;
import com.edutech.center.application.service.BatchService;
import com.edutech.center.domain.model.BatchStatus;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/centers/{centerId}/batches")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Batches", description = "Batch management within a center")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new batch")
    public BatchResponse createBatch(@PathVariable UUID centerId,
                                     @Valid @RequestBody CreateBatchRequest request,
                                     @AuthenticationPrincipal AuthPrincipal principal) {
        return batchService.createBatch(centerId, request, principal);
    }

    @GetMapping
    @Operation(summary = "List batches for a center")
    public List<BatchResponse> listBatches(@PathVariable UUID centerId,
                                           @RequestParam(required = false) BatchStatus status,
                                           @AuthenticationPrincipal AuthPrincipal principal) {
        return batchService.listBatches(centerId, status, principal);
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "Get batch details")
    public BatchResponse getBatch(@PathVariable UUID centerId,
                                  @PathVariable UUID batchId,
                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return batchService.getBatch(centerId, batchId, principal);
    }

    @PutMapping("/{batchId}")
    @Operation(summary = "Update batch (status, teacher assignment)")
    public BatchResponse updateBatch(@PathVariable UUID centerId,
                                     @PathVariable UUID batchId,
                                     @Valid @RequestBody UpdateBatchRequest request,
                                     @AuthenticationPrincipal AuthPrincipal principal) {
        return batchService.updateBatch(batchId, request, principal);
    }
}
