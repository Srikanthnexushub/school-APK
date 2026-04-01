// src/main/java/com/edutech/center/infrastructure/persistence/BatchPersistenceAdapter.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.Batch;
import com.edutech.center.domain.model.BatchStatus;
import com.edutech.center.domain.port.out.BatchRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BatchPersistenceAdapter implements BatchRepository {

    private final SpringDataBatchRepository jpa;

    public BatchPersistenceAdapter(SpringDataBatchRepository jpa) { this.jpa = jpa; }

    @Override public Batch save(Batch batch) { return jpa.save(batch); }
    @Override public Optional<Batch> findById(UUID id) { return jpa.findById(id); }
    @Override public List<Batch> findByCenterId(UUID centerId) { return jpa.findByCenterIdActive(centerId); }
    @Override public List<Batch> findByCenterIdAndStatus(UUID centerId, BatchStatus status) { return jpa.findByCenterIdAndStatusActive(centerId, status); }
    @Override public Optional<Batch> findByIdAndCenterId(UUID id, UUID centerId) { return jpa.findByIdAndCenterIdActive(id, centerId); }
    @Override public long countAllActive() { return jpa.countAllActive(); }
    @Override public long countByStatusActive() { return jpa.countByStatusActive(); }
    @Override public long countByStatusUpcoming() { return jpa.countByStatusUpcoming(); }
    @Override public long countByStatusCompleted() { return jpa.countByStatusCompleted(); }
    @Override public Long sumEnrolledCount() { return jpa.sumEnrolledCount(); }
    @Override public Double avgFillRate() { return jpa.avgFillRate(); }
}
