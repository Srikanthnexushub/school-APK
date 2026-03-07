// src/main/java/com/edutech/parent/api/FeePaymentController.java
package com.edutech.parent.api;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.FeePaymentResponse;
import com.edutech.parent.application.dto.RecordFeePaymentRequest;
import com.edutech.parent.application.service.FeePaymentService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parents/{profileId}/payments")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Fee Payments", description = "Fee payment record management")
public class FeePaymentController {

    private final FeePaymentService feePaymentService;

    public FeePaymentController(FeePaymentService feePaymentService) {
        this.feePaymentService = feePaymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a fee payment")
    public FeePaymentResponse recordPayment(
            @PathVariable UUID profileId,
            @Valid @RequestBody RecordFeePaymentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return feePaymentService.recordPayment(profileId, request, principal);
    }

    @GetMapping
    @Operation(summary = "List all fee payments for this parent profile")
    public List<FeePaymentResponse> listPayments(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return feePaymentService.listPayments(profileId, principal);
    }
}
