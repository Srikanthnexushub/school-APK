// src/main/java/com/edutech/parent/api/ParentController.java
package com.edutech.parent.api;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.CreateParentProfileRequest;
import com.edutech.parent.application.dto.ParentProfileResponse;
import com.edutech.parent.application.dto.RequestParentLinkRequest;
import com.edutech.parent.application.dto.StudentLinkResponse;
import com.edutech.parent.application.dto.UpdateParentProfileRequest;
import com.edutech.parent.application.service.ParentProfileService;
import com.edutech.parent.application.service.StudentLinkService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parents")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Parent Profiles", description = "Parent profile management")
public class ParentController {

    private final ParentProfileService parentProfileService;
    private final StudentLinkService studentLinkService;

    public ParentController(ParentProfileService parentProfileService,
                            StudentLinkService studentLinkService) {
        this.parentProfileService = parentProfileService;
        this.studentLinkService = studentLinkService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a parent profile for the authenticated user")
    public ParentProfileResponse createProfile(
            @Valid @RequestBody CreateParentProfileRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return parentProfileService.createProfile(request, principal);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated parent's own profile")
    public ParentProfileResponse getMyProfile(@AuthenticationPrincipal AuthPrincipal principal) {
        return parentProfileService.getMyProfile(principal);
    }

    @GetMapping("/{profileId}")
    @Operation(summary = "Get a parent profile by ID")
    public ParentProfileResponse getProfile(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return parentProfileService.getProfile(profileId, principal);
    }

    @PutMapping("/{profileId}")
    @Operation(summary = "Update a parent profile")
    public ParentProfileResponse updateProfile(
            @PathVariable UUID profileId,
            @Valid @RequestBody UpdateParentProfileRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return parentProfileService.updateProfile(profileId, request, principal);
    }

    @PostMapping("/request-link")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Request a parent link (called by a student to link themselves to a parent by parent email)")
    public StudentLinkResponse requestLink(
            @Valid @RequestBody RequestParentLinkRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return studentLinkService.requestLinkFromStudent(request.parentEmail(), principal.userId());
    }
}
