// src/main/java/com/edutech/center/api/FeeController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CreateFeeStructureRequest;
import com.edutech.center.application.dto.FeeStructureResponse;
import com.edutech.center.application.service.FeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/centers/{centerId}/fees")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Fee Structures", description = "Fee plan management")
public class FeeController {

    private final FeeService feeService;

    public FeeController(FeeService feeService) {
        this.feeService = feeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a fee structure for a center")
    public FeeStructureResponse createFeeStructure(@PathVariable UUID centerId,
                                                   @Valid @RequestBody CreateFeeStructureRequest request,
                                                   @AuthenticationPrincipal AuthPrincipal principal) {
        return feeService.createFeeStructure(centerId, request, principal);
    }

    @GetMapping
    @Operation(summary = "List active fee structures for a center")
    public Page<FeeStructureResponse> listFeeStructures(@PathVariable UUID centerId,
                                                        @AuthenticationPrincipal AuthPrincipal principal,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "50") int size) {
        return feeService.listFeeStructures(centerId, principal, PageRequest.of(page, size));
    }
}
