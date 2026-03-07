// src/main/java/com/edutech/center/api/AttendanceController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AttendanceResponse;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.MarkAttendanceRequest;
import com.edutech.center.application.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/centers/{centerId}/batches/{batchId}/attendance")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Attendance", description = "Student attendance tracking")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Mark attendance for a batch on a date (re-marking replaces existing)")
    public List<AttendanceResponse> markAttendance(@PathVariable UUID centerId,
                                                   @PathVariable UUID batchId,
                                                   @Valid @RequestBody MarkAttendanceRequest request,
                                                   @AuthenticationPrincipal AuthPrincipal principal) {
        return attendanceService.markAttendance(batchId, request, principal);
    }

    @GetMapping
    @Operation(summary = "Get attendance for a batch on a specific date")
    public List<AttendanceResponse> getAttendance(@PathVariable UUID centerId,
                                                  @PathVariable UUID batchId,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return attendanceService.getAttendance(batchId, date, principal);
    }
}
