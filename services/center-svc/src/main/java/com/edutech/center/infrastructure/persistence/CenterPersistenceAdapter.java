// src/main/java/com/edutech/center/infrastructure/persistence/CenterPersistenceAdapter.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.CoachingCenter;
import com.edutech.center.domain.port.out.CenterRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CenterPersistenceAdapter implements CenterRepository {

    private final SpringDataCenterRepository jpa;

    public CenterPersistenceAdapter(SpringDataCenterRepository jpa) {
        this.jpa = jpa;
    }

    @Override public CoachingCenter save(CoachingCenter center) { return jpa.save(center); }
    @Override public Optional<CoachingCenter> findById(UUID id) { return jpa.findByIdActive(id); }
    @Override public List<CoachingCenter> findAll() { return jpa.findAllActive(); }
    @Override public List<CoachingCenter> findByOwnerId(UUID ownerId) { return jpa.findByOwnerIdActive(ownerId); }
    @Override public boolean existsByCode(String code) { return jpa.existsByCode(code); }
}
