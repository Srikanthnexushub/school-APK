// src/main/java/com/edutech/center/api/ScheduleController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CreateScheduleRequest;
import com.edutech.center.application.dto.ScheduleResponse;
import com.edutech.center.application.service.ScheduleService;
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
@RequestMapping("/api/v1/centers/{centerId}/batches/{batchId}/schedules")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Schedules", description = "Batch schedule management")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a schedule slot to a batch")
    public ScheduleResponse createSchedule(@PathVariable UUID centerId,
                                           @PathVariable UUID batchId,
                                           @Valid @RequestBody CreateScheduleRequest request,
                                           @AuthenticationPrincipal AuthPrincipal principal) {
        return scheduleService.createSchedule(batchId, request, principal);
    }

    @GetMapping
    @Operation(summary = "List schedule slots for a batch")
    public List<ScheduleResponse> listSchedules(@PathVariable UUID centerId,
                                                @PathVariable UUID batchId,
                                                @AuthenticationPrincipal AuthPrincipal principal) {
        return scheduleService.listSchedules(batchId, principal);
    }
}
