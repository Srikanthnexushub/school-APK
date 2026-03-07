// src/main/java/com/edutech/center/api/CenterController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.CreateCenterRequest;
import com.edutech.center.application.dto.UpdateCenterRequest;
import com.edutech.center.application.service.CenterService;
import com.edutech.center.domain.port.in.CreateCenterUseCase;
import com.edutech.center.domain.port.in.UpdateCenterUseCase;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a coaching center (SUPER_ADMIN only)")
    public CenterResponse createCenter(@Valid @RequestBody CreateCenterRequest request,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return createCenterUseCase.createCenter(request, principal);
    }

    @GetMapping
    @Operation(summary = "List centers accessible to the caller")
    public List<CenterResponse> listCenters(@AuthenticationPrincipal AuthPrincipal principal) {
        return centerService.listCenters(principal);
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
}
