// src/main/java/com/edutech/parent/api/NotificationPreferenceController.java
package com.edutech.parent.api;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.CreateNotificationPreferenceRequest;
import com.edutech.parent.application.dto.NotificationPreferenceResponse;
import com.edutech.parent.application.dto.UpdateNotificationPreferenceRequest;
import com.edutech.parent.application.service.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parents/{profileId}/notification-preferences")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Notification Preferences", description = "Parent notification preference management")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    public NotificationPreferenceController(NotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @PostMapping
    @Operation(summary = "Create or update a notification preference (upsert by channel + eventType)")
    public NotificationPreferenceResponse upsertPreference(
            @PathVariable UUID profileId,
            @Valid @RequestBody CreateNotificationPreferenceRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return preferenceService.upsertPreference(profileId, request, principal);
    }

    @GetMapping
    @Operation(summary = "List all notification preferences for this parent")
    public List<NotificationPreferenceResponse> getPreferences(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return preferenceService.getPreferences(profileId, principal);
    }

    @PutMapping("/{prefId}")
    @Operation(summary = "Update a specific notification preference")
    public NotificationPreferenceResponse updatePreference(
            @PathVariable UUID profileId,
            @PathVariable UUID prefId,
            @Valid @RequestBody UpdateNotificationPreferenceRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return preferenceService.updatePreference(profileId, prefId, request, principal);
    }
}
