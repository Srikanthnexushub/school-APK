package com.edutech.aimentor.api;

import com.edutech.aimentor.application.dto.ReminderRequest;
import com.edutech.aimentor.application.dto.ReminderResponse;
import com.edutech.aimentor.application.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reminders")
@Tag(name = "Reminders", description = "Study plan reminders for Academic Plan")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a reminder for a study plan")
    public ResponseEntity<ReminderResponse> create(
            @Valid @RequestBody ReminderRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reminderService.createReminder(userId, request));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get all due (unacknowledged) reminders for the authenticated student")
    public ResponseEntity<List<ReminderResponse>> getPending(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ResponseEntity.ok(reminderService.getPendingReminders(userId));
    }

    @PutMapping("/{reminderId}/acknowledge")
    @Operation(summary = "Acknowledge (dismiss) a reminder")
    public ResponseEntity<ReminderResponse> acknowledge(
            @PathVariable UUID reminderId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ResponseEntity.ok(reminderService.acknowledge(reminderId, userId));
    }
}
