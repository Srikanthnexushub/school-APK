// src/main/java/com/edutech/center/api/AnnouncementController.java
package com.edutech.center.api;

import com.edutech.center.application.command.SendAnnouncementCommand;
import com.edutech.center.application.dto.AnnouncementResponse;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.service.AnnouncementService;
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
@RequestMapping("/api/v1/centers/{centerId}/announcements")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Announcements", description = "Targeted announcements to batch, center, or role audiences")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send an announcement (CENTER_ADMIN / TEACHER / INSTITUTION_ADMIN)")
    public AnnouncementResponse send(@PathVariable UUID centerId,
                                     @Valid @RequestBody SendAnnouncementCommand command,
                                     @AuthenticationPrincipal AuthPrincipal principal) {
        SendAnnouncementCommand enriched = new SendAnnouncementCommand(
                centerId, command.title(), command.body(),
                command.targetType(), command.targetId(), command.targetRole(),
                command.scheduledAt(), command.expiresAt());
        return announcementService.send(enriched, principal);
    }

    @GetMapping
    @Operation(summary = "List active announcements for a center")
    public List<AnnouncementResponse> list(@PathVariable UUID centerId,
                                           @AuthenticationPrincipal AuthPrincipal principal) {
        return announcementService.listByCenterId(centerId, principal);
    }

    @DeleteMapping("/{announcementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete (expire) an announcement")
    public void expire(@PathVariable UUID centerId,
                       @PathVariable UUID announcementId,
                       @AuthenticationPrincipal AuthPrincipal principal) {
        announcementService.expire(centerId, announcementId, principal);
    }
}
