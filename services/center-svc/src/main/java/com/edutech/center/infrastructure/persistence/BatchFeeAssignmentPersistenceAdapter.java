// src/main/java/com/edutech/center/infrastructure/persistence/BatchFeeAssignmentPersistenceAdapter.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.BatchFeeAssignment;
import com.edutech.center.domain.port.out.BatchFeeAssignmentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BatchFeeAssignmentPersistenceAdapter implements BatchFeeAssignmentRepository {

    private final SpringDataBatchFeeAssignmentRepository jpa;

    public BatchFeeAssignmentPersistenceAdapter(SpringDataBatchFeeAssignmentRepository jpa) { this.jpa = jpa; }

    @Override public BatchFeeAssignment save(BatchFeeAssignment a) { return jpa.save(a); }
    @Override public Optional<BatchFeeAssignment> findById(UUID id) { return jpa.findById(id); }
    @Override public List<BatchFeeAssignment> findByBatchId(UUID batchId) { return jpa.findByBatchId(batchId); }
    @Override public List<BatchFeeAssignment> findByCenterId(UUID centerId) { return jpa.findByCenterId(centerId); }
    @Override public void deleteById(UUID id) { jpa.deleteById(id); }
}
