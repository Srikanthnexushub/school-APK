// src/main/java/com/edutech/center/application/service/BannerService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BannerResponse;
import com.edutech.center.application.dto.CreateBannerRequest;
import com.edutech.center.application.dto.UpdateBannerRequest;
import com.edutech.center.application.exception.BannerNotFoundException;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.domain.model.Banner;
import com.edutech.center.domain.model.BannerAudience;
import com.edutech.center.domain.model.BannerType;
import com.edutech.center.domain.port.out.BannerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for platform-level banner management.
 *
 * <p>Write operations (create, update, toggle, delete) are restricted to SUPER_ADMIN.
 * Read operations (audience-filtered list) are available to any authenticated user.
 */
@Service
@Transactional
public class BannerService {

    private static final Logger log = LoggerFactory.getLogger(BannerService.class);

    private final BannerRepository bannerRepository;

    public BannerService(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    /**
     * Creates a new platform banner. SUPER_ADMIN only.
     */
    public BannerResponse createBanner(CreateBannerRequest request, AuthPrincipal principal) {
        if (!principal.isSuperAdmin()) throw new CenterAccessDeniedException();
        Banner banner = Banner.create(
                request.title(),
                request.subtitle(),
                request.imageUrl(),
                request.linkUrl(),
                request.linkLabel(),
                request.audience(),
                request.bgColor(),
                request.displayOrder(),
                request.startDate(),
                request.endDate(),
                request.bannerType());
        Banner saved = bannerRepository.save(banner);
        log.info("Banner created: id={} audience={} title={}", saved.getId(), request.audience(), request.title());
        return toResponse(saved);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    /**
     * Updates editable banner details (PATCH semantics). SUPER_ADMIN only.
     */
    public BannerResponse updateBanner(UUID id, UpdateBannerRequest request, AuthPrincipal principal) {
        if (!principal.isSuperAdmin()) throw new CenterAccessDeniedException();
        Banner banner = bannerRepository.findActiveById(id)
                .orElseThrow(() -> new BannerNotFoundException(id));
        banner.updateDetails(
                request.title(),
                request.subtitle(),
                request.imageUrl(),
                request.linkUrl(),
                request.linkLabel(),
                request.bgColor(),
                request.displayOrder() != null ? request.displayOrder() : banner.getDisplayOrder(),
                request.startDate(),
                request.endDate());
        if (request.audience() != null) {
            banner.setAudience(request.audience());
        }
        if (request.bannerType() != null) {
            banner.setBannerType(request.bannerType());
        }
        Banner saved = bannerRepository.save(banner);
        log.info("Banner updated: id={}", id);
        return toResponse(saved);
    }

    // ─── Toggle active ─────────────────────────────────────────────────────────

    /**
     * Toggles the active state of a banner. SUPER_ADMIN only.
     */
    public BannerResponse toggleActive(UUID id, AuthPrincipal principal) {
        if (!principal.isSuperAdmin()) throw new CenterAccessDeniedException();
        Banner banner = bannerRepository.findActiveById(id)
                .orElseThrow(() -> new BannerNotFoundException(id));
        if (banner.isActive()) {
            banner.deactivate();
        } else {
            banner.activate();
        }
        Banner saved = bannerRepository.save(banner);
        log.info("Banner toggled: id={} isActive={}", id, saved.isActive());
        return toResponse(saved);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    /**
     * Soft-deletes a banner. SUPER_ADMIN only.
     */
    public void deleteBanner(UUID id, AuthPrincipal principal) {
        if (!principal.isSuperAdmin()) throw new CenterAccessDeniedException();
        Banner banner = bannerRepository.findActiveById(id)
                .orElseThrow(() -> new BannerNotFoundException(id));
        banner.softDelete();
        bannerRepository.save(banner);
        log.info("Banner soft-deleted: id={}", id);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    /**
     * Returns active banners targeted at the given audience.
     * Available to any authenticated user.
     */
    @Transactional(readOnly = true)
    public List<BannerResponse> getActiveBannersForAudience(String audienceParam,
                                                             AuthPrincipal principal) {
        BannerAudience audience;
        try {
            audience = BannerAudience.valueOf(audienceParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid audience: " + audienceParam);
        }
        return bannerRepository.findActiveByAudience(audience).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns all non-deleted banners for the SUPER_ADMIN management view.
     * SUPER_ADMIN only.
     */
    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners(AuthPrincipal principal) {
        if (!principal.isSuperAdmin()) throw new CenterAccessDeniedException();
        return bannerRepository.findAllActive().stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private BannerResponse toResponse(Banner b) {
        return new BannerResponse(
                b.getId(),
                b.getTitle(),
                b.getSubtitle(),
                b.getImageUrl(),
                b.getLinkUrl(),
                b.getLinkLabel(),
                b.getAudience(),
                b.getBannerType(),
                b.getBgColor(),
                b.getDisplayOrder(),
                b.isActive(),
                b.getStartDate(),
                b.getEndDate(),
                b.getCreatedAt());
    }
}
