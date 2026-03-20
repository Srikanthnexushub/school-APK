// src/main/java/com/edutech/center/domain/port/out/BannerRepository.java
package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.Banner;
import com.edutech.center.domain.model.BannerAudience;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (output) — persistence contract for {@link Banner} aggregate.
 * Implemented by {@code BannerPersistenceAdapter} in the infrastructure layer.
 */
public interface BannerRepository {

    /** Finds any banner by its ID regardless of soft-deletion state. */
    Optional<Banner> findById(UUID id);

    /**
     * Finds a non-deleted banner by its ID.
     * Returns empty if the banner does not exist or has been soft-deleted.
     */
    Optional<Banner> findActiveById(UUID id);

    /**
     * Returns active, non-deleted banners targeted at the given audience
     * (including banners with audience=ALL), ordered by displayOrder ASC.
     * Only banners within their optional start/end date window are included.
     */
    List<Banner> findActiveByAudience(BannerAudience audience);

    /**
     * Returns all non-deleted banners regardless of active flag or date window,
     * ordered by displayOrder ASC. Intended for the SUPER_ADMIN management view.
     */
    List<Banner> findAllActive();

    /** Persists a new or updated {@link Banner} and returns the managed instance. */
    Banner save(Banner banner);
}
