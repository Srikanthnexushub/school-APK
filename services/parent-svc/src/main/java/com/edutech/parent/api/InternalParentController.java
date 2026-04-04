// src/main/java/com/edutech/parent/api/InternalParentController.java
package com.edutech.parent.api;

import com.edutech.parent.application.service.InternalParentQueryService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Internal service-to-service endpoints for parent-svc.
 * Secured exclusively via X-Service-Key header (ServiceKeyAuthFilter).
 * NOT exposed through api-gateway or student-gateway.
 */
@RestController
@RequestMapping("/api/v1/internal/parents")
@Hidden
public class InternalParentController {

    private final InternalParentQueryService internalParentQueryService;

    public InternalParentController(InternalParentQueryService internalParentQueryService) {
        this.internalParentQueryService = internalParentQueryService;
    }

    /**
     * Returns the list of distinct center IDs where the parent has at least one
     * active student link. Used by center-svc to validate PARENT access to
     * center-scoped resources (library content, fee structures).
     *
     * @param parentUserId the parent's auth userId (from JWT sub claim)
     * @return list of center UUIDs (may be empty; never null)
     */
    @GetMapping("/{parentUserId}/linked-center-ids")
    public List<UUID> getLinkedCenterIds(@PathVariable UUID parentUserId) {
        return internalParentQueryService.getLinkedCenterIds(parentUserId);
    }
}
