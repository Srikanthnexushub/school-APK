// src/main/java/com/edutech/center/api/AttendanceSummaryController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AttendanceSummaryResponse;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.service.AttendanceSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/centers/{centerId}/batches/{batchId}/attendance")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Attendance Summary", description = "Attendance percentage and statistics per student")
public class AttendanceSummaryController {

    private final AttendanceSummaryService summaryService;

    public AttendanceSummaryController(AttendanceSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Attendance summary for all active students in a batch (TEACHER / CENTER_ADMIN)")
    public List<AttendanceSummaryResponse> getSummary(@PathVariable UUID centerId,
                                                      @PathVariable UUID batchId,
                                                      @AuthenticationPrincipal AuthPrincipal principal) {
        return summaryService.getSummary(batchId, principal);
    }

    @GetMapping("/my-summary")
    @Operation(summary = "Requesting student's own attendance summary for this batch")
    public AttendanceSummaryResponse getMyAttendance(@PathVariable UUID centerId,
                                                     @PathVariable UUID batchId,
                                                     @AuthenticationPrincipal AuthPrincipal principal) {
        return summaryService.getMyAttendance(batchId, principal);
    }
}
