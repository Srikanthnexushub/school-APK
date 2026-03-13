// src/main/java/com/edutech/center/api/CenterController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CenterLookupResponse;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.CreateCenterRequest;
import com.edutech.center.application.dto.InstitutionRegistrationRequest;
import com.edutech.center.application.dto.InstitutionRegistrationResponse;
import com.edutech.center.application.dto.RejectInstitutionRequest;
import com.edutech.center.application.dto.RegistrationStatusResponse;
import com.edutech.center.application.dto.UpdateCenterRequest;
import com.edutech.center.application.service.CenterService;
import com.edutech.center.domain.port.in.CreateCenterUseCase;
import com.edutech.center.domain.port.in.UpdateCenterUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/centers")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Centers", description = "Coaching center management")
public class CenterController {

    private final CreateCenterUseCase createCenterUseCase;
    private final UpdateCenterUseCase updateCenterUseCase;
    private final CenterService centerService;

    public CenterController(CreateCenterUseCase createCenterUseCase,
                            UpdateCenterUseCase updateCenterUseCase,
                            CenterService centerService) {
        this.createCenterUseCase = createCenterUseCase;
        this.updateCenterUseCase = updateCenterUseCase;
        this.centerService = centerService;
    }

    @GetMapping("/lookup")
    @Operation(summary = "Look up a center by institution code — public, no auth required")
    public ResponseEntity<CenterLookupResponse> lookupByCode(@RequestParam String code) {
        return centerService.lookupByCode(code)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a coaching center (SUPER_ADMIN only)")
    public CenterResponse createCenter(@Valid @RequestBody CreateCenterRequest request,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return createCenterUseCase.createCenter(request, principal);
    }

    @GetMapping
    @Operation(summary = "List centers accessible to the caller")
    public Page<CenterResponse> listCenters(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return centerService.listCenters(principal, PageRequest.of(page, size));
    }

    @GetMapping("/{centerId}")
    @Operation(summary = "Get center details")
    public CenterResponse getCenter(@PathVariable UUID centerId,
                                    @AuthenticationPrincipal AuthPrincipal principal) {
        return centerService.getCenter(centerId, principal);
    }

    @PutMapping("/{centerId}")
    @Operation(summary = "Update center details")
    public CenterResponse updateCenter(@PathVariable UUID centerId,
                                       @Valid @RequestBody UpdateCenterRequest request,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return updateCenterUseCase.updateCenter(centerId, request, principal);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Self-register an institution — starts PENDING_VERIFICATION")
    public InstitutionRegistrationResponse registerInstitution(
            @Valid @RequestBody InstitutionRegistrationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return centerService.registerInstitution(request, principal);
    }

    @GetMapping("/pending")
    @Operation(summary = "List pending institution registrations (SUPER_ADMIN only)")
    public List<CenterResponse> listPendingRegistrations(@AuthenticationPrincipal AuthPrincipal principal) {
        return centerService.listPendingRegistrations(principal);
    }

    @PostMapping("/{centerId}/approve")
    @Operation(summary = "Approve an institution registration (SUPER_ADMIN only)")
    public CenterResponse approveRegistration(@PathVariable UUID centerId,
                                              @AuthenticationPrincipal AuthPrincipal principal) {
        return centerService.approveCenterRegistration(centerId, principal);
    }

    @PostMapping("/{centerId}/reject")
    @Operation(summary = "Reject an institution registration (SUPER_ADMIN only)")
    public CenterResponse rejectRegistration(@PathVariable UUID centerId,
                                             @Valid @RequestBody RejectInstitutionRequest request,
                                             @AuthenticationPrincipal AuthPrincipal principal) {
        return centerService.rejectCenterRegistration(centerId, request, principal);
    }

    @GetMapping("/my-registration")
    @Operation(summary = "Get the current user's institution registration status")
    public RegistrationStatusResponse getMyRegistration(@AuthenticationPrincipal AuthPrincipal principal) {
        return centerService.getMyCenterRegistration(principal);
    }
}
