// src/main/java/com/edutech/center/infrastructure/persistence/AnnouncementPersistenceAdapter.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.Announcement;
import com.edutech.center.domain.port.out.AnnouncementRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AnnouncementPersistenceAdapter implements AnnouncementRepository {

    private final SpringDataAnnouncementRepository jpa;

    public AnnouncementPersistenceAdapter(SpringDataAnnouncementRepository jpa) { this.jpa = jpa; }

    @Override public Announcement save(Announcement a) { return jpa.save(a); }
    @Override public Optional<Announcement> findById(UUID id) { return jpa.findById(id); }
    @Override public List<Announcement> findActiveByCenterId(UUID centerId) { return jpa.findActiveByCenterId(centerId, Instant.now()); }
    @Override public List<Announcement> findDue() { return jpa.findDue(Instant.now()); }
}
