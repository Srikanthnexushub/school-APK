// src/main/java/com/edutech/parent/api/BillingReportController.java
package com.edutech.parent.api;

import com.edutech.parent.application.dto.AdminRecordPaymentRequest;
import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.FeePaymentResponse;
import com.edutech.parent.application.dto.StudentFeeReportItem;
import com.edutech.parent.application.service.BillingReportService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parents/billing")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Billing Report", description = "Admin fee billing report and parent reminders")
public class BillingReportController {

    private final BillingReportService billingReportService;

    public BillingReportController(BillingReportService billingReportService) {
        this.billingReportService = billingReportService;
    }

    @GetMapping("/report")
    @Operation(summary = "Get fee payment report for all students in a center")
    public List<StudentFeeReportItem> getReport(
            @RequestParam UUID centerId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return billingReportService.getBillingReport(centerId, principal);
    }

    @PostMapping("/record-payment")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Admin: record a confirmed fee payment for a student (resolves parent profile automatically)")
    public FeePaymentResponse recordPayment(
            @Valid @RequestBody AdminRecordPaymentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return billingReportService.recordPaymentForAdmin(request, principal);
    }

    @PostMapping("/reminder/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Send fee payment reminder notification to the parent of a student")
    public void sendReminder(
            @PathVariable UUID studentId,
            @RequestParam UUID centerId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        billingReportService.sendFeeReminder(centerId, studentId, principal);
    }
}
