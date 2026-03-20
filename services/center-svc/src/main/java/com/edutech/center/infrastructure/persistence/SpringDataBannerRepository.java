// src/main/java/com/edutech/center/infrastructure/persistence/SpringDataBannerRepository.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.Banner;
import com.edutech.center.domain.model.BannerAudience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Banner}.
 * Package-private — accessed only through {@link BannerPersistenceAdapter}.
 */
interface SpringDataBannerRepository extends JpaRepository<Banner, UUID> {

    @Query("SELECT b FROM Banner b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Banner> findActiveById(@Param("id") UUID id);

    @Query("""
            SELECT b FROM Banner b
            WHERE b.deletedAt IS NULL
              AND b.isActive = TRUE
              AND (b.audience = :audience OR b.audience = com.edutech.center.domain.model.BannerAudience.ALL)
              AND (b.startDate IS NULL OR b.startDate <= :now)
              AND (b.endDate IS NULL OR b.endDate >= :now)
            ORDER BY b.displayOrder ASC
            """)
    List<Banner> findActiveByAudience(@Param("audience") BannerAudience audience,
                                      @Param("now") Instant now);

    @Query("SELECT b FROM Banner b WHERE b.deletedAt IS NULL ORDER BY b.displayOrder ASC")
    List<Banner> findAllActive();
}
