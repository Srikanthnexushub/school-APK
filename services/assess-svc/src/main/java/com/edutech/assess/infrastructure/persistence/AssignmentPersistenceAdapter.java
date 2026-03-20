// src/main/java/com/edutech/assess/infrastructure/persistence/AssignmentPersistenceAdapter.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.Assignment;
import com.edutech.assess.domain.port.out.AssignmentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class AssignmentPersistenceAdapter implements AssignmentRepository {

    private final SpringDataAssignmentRepository repository;

    AssignmentPersistenceAdapter(SpringDataAssignmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Assignment> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Assignment> findActiveById(UUID id) {
        return repository.findActiveById(id);
    }

    @Override
    public List<Assignment> findByBatchIdActive(UUID batchId) {
        return repository.findByBatchIdActive(batchId);
    }

    @Override
    public List<Assignment> findByCenterIdActive(UUID centerId) {
        return repository.findByCenterIdActive(centerId);
    }

    @Override
    public List<Assignment> findPublishedByBatchId(UUID batchId) {
        return repository.findPublishedByBatchId(batchId);
    }

    @Override
    public Assignment save(Assignment assignment) {
        return repository.save(assignment);
    }

    @Override
    public void delete(Assignment assignment) {
        assignment.softDelete();
        repository.save(assignment);
    }
}
