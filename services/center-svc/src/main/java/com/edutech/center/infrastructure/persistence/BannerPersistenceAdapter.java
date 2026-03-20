// src/main/java/com/edutech/center/infrastructure/persistence/BannerPersistenceAdapter.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.Banner;
import com.edutech.center.domain.model.BannerAudience;
import com.edutech.center.domain.port.out.BannerRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter that bridges the domain {@link BannerRepository} port
 * and the Spring Data JPA {@link SpringDataBannerRepository}.
 */
@Component
public class BannerPersistenceAdapter implements BannerRepository {

    private final SpringDataBannerRepository jpa;

    public BannerPersistenceAdapter(SpringDataBannerRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Banner> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Banner> findActiveById(UUID id) {
        return jpa.findActiveById(id);
    }

    @Override
    public List<Banner> findActiveByAudience(BannerAudience audience) {
        return jpa.findActiveByAudience(audience, Instant.now());
    }

    @Override
    public List<Banner> findAllActive() {
        return jpa.findAllActive();
    }

    @Override
    public Banner save(Banner banner) {
        return jpa.save(banner);
    }
}
