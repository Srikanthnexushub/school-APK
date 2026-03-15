// src/main/java/com/edutech/parent/infrastructure/persistence/ParentProfilePersistenceAdapter.java
package com.edutech.parent.infrastructure.persistence;

import com.edutech.parent.domain.model.ParentProfile;
import com.edutech.parent.domain.port.out.ParentProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class ParentProfilePersistenceAdapter implements ParentProfileRepository {

    private final SpringDataParentProfileRepository repository;

    ParentProfilePersistenceAdapter(SpringDataParentProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public ParentProfile save(ParentProfile profile) {
        return repository.save(profile);
    }

    @Override
    public Optional<ParentProfile> findById(UUID id) {
        return repository.findByIdActive(id);
    }

    @Override
    public Optional<ParentProfile> findByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public Optional<ParentProfile> findByEmail(String email) {
        return repository.findByEmailActive(email);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return repository.existsByUserId(userId);
    }
}
