// src/main/java/com/edutech/center/api/StaffController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CreateStaffRequest;
import com.edutech.center.application.dto.TeacherResponse;
import com.edutech.center.application.dto.UpdateStaffRequest;
import com.edutech.center.application.service.StaffService;
import com.edutech.center.domain.model.StaffRoleType;
import com.edutech.center.domain.model.TeacherStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for role-based staff management within a coaching center.
 *
 * <p>All endpoints require a valid JWT and CENTER_ADMIN / SUPER_ADMIN role.
 * Base path: {@code /api/v1/centers/{centerId}/staff}
 */
@RestController
@RequestMapping("/api/v1/centers/{centerId}/staff")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Staff", description = "Role-based staff management (create, update, deactivate, list) within a center")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    /**
     * Create a staff member by admin-initiated invitation.
     * Sends an email invitation via the notification service.
     * The record is created in INVITATION_SENT status.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a staff member by invitation (admin-initiated)")
    public TeacherResponse createStaff(@PathVariable UUID centerId,
                                       @Valid @RequestBody CreateStaffRequest request,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return staffService.createStaff(centerId, request, principal);
    }

    /**
     * List all staff for a center with optional filters.
     *
     * @param roleType filter by role type (e.g. TEACHER, HOD, COUNSELOR)
     * @param status   filter by status (e.g. ACTIVE, INVITATION_SENT, PENDING_APPROVAL)
     */
    @GetMapping
    @Operation(summary = "List staff for a center, optionally filtered by roleType and/or status")
    public List<TeacherResponse> listStaff(@PathVariable UUID centerId,
                                           @RequestParam(required = false) StaffRoleType roleType,
                                           @RequestParam(required = false) TeacherStatus status,
                                           @AuthenticationPrincipal AuthPrincipal principal) {
        return staffService.listStaff(centerId, principal, roleType, status);
    }

    /**
     * Partially update a staff member's profile (PATCH semantics).
     * Only non-null fields in the request body are applied.
     */
    @PatchMapping("/{staffId}")
    @Operation(summary = "Partially update a staff member's profile")
    public TeacherResponse updateStaff(@PathVariable UUID centerId,
                                       @PathVariable UUID staffId,
                                       @Valid @RequestBody UpdateStaffRequest request,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return staffService.updateStaff(centerId, staffId, request, principal);
    }

    /**
     * Deactivate (soft-delete) a staff member.
     * Sets status to INACTIVE and stamps deletedAt.
     */
    @DeleteMapping("/{staffId}")
    @Operation(summary = "Deactivate a staff member (soft delete)")
    public TeacherResponse deactivateStaff(@PathVariable UUID centerId,
                                           @PathVariable UUID staffId,
                                           @AuthenticationPrincipal AuthPrincipal principal) {
        return staffService.deactivateStaff(centerId, staffId, principal);
    }

    /**
     * Re-activate a previously deactivated staff member.
     */
    @PostMapping("/{staffId}/reactivate")
    @Operation(summary = "Re-activate a previously deactivated staff member")
    public TeacherResponse reactivateStaff(@PathVariable UUID centerId,
                                           @PathVariable UUID staffId,
                                           @AuthenticationPrincipal AuthPrincipal principal) {
        return staffService.reactivateStaff(centerId, staffId, principal);
    }
}
