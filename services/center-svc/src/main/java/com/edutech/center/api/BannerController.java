// src/main/java/com/edutech/center/api/BannerController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BannerResponse;
import com.edutech.center.application.dto.CreateBannerRequest;
import com.edutech.center.application.dto.UpdateBannerRequest;
import com.edutech.center.application.service.BannerService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for platform-level advertisement banner management.
 *
 * <p>Read endpoints are available to any authenticated user.
 * Write endpoints (create / update / toggle / delete) require SUPER_ADMIN — enforced
 * in {@link BannerService}.
 */
@RestController
@RequestMapping("/api/v1/banners")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Banners", description = "Platform-level promotional banner management (SUPER_ADMIN) and public read")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    /**
     * Returns active banners filtered by audience.
     * Any authenticated user may call this endpoint.
     *
     * @param audience target audience (PARENT, CENTER_ADMIN, or ALL); defaults to ALL
     */
    @GetMapping
    @Operation(summary = "List active banners for the given audience (any authenticated user)")
    public List<BannerResponse> getActiveBannersForAudience(
            @RequestParam(defaultValue = "ALL") String audience,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return bannerService.getActiveBannersForAudience(audience, principal);
    }

    /**
     * Returns all non-deleted banners for the SUPER_ADMIN management view.
     */
    @GetMapping("/all")
    @Operation(summary = "List all banners — management view (SUPER_ADMIN only)")
    public List<BannerResponse> getAllBanners(@AuthenticationPrincipal AuthPrincipal principal) {
        return bannerService.getAllBanners(principal);
    }

    /**
     * Creates a new platform banner. SUPER_ADMIN only.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a platform banner (SUPER_ADMIN only)")
    public BannerResponse createBanner(@Valid @RequestBody CreateBannerRequest request,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return bannerService.createBanner(request, principal);
    }

    /**
     * Updates editable banner details (PATCH semantics). SUPER_ADMIN only.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a platform banner (SUPER_ADMIN only)")
    public BannerResponse updateBanner(@PathVariable UUID id,
                                       @RequestBody UpdateBannerRequest request,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return bannerService.updateBanner(id, request, principal);
    }

    /**
     * Toggles the active/inactive state of a banner. SUPER_ADMIN only.
     */
    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle banner active state (SUPER_ADMIN only)")
    public BannerResponse toggleActive(@PathVariable UUID id,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return bannerService.toggleActive(id, principal);
    }

    /**
     * Soft-deletes a banner. The record is retained for audit purposes. SUPER_ADMIN only.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a platform banner (SUPER_ADMIN only)")
    public void deleteBanner(@PathVariable UUID id,
                             @AuthenticationPrincipal AuthPrincipal principal) {
        bannerService.deleteBanner(id, principal);
    }
}
