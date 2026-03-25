// src/main/java/com/edutech/center/domain/port/out/BatchFeeAssignmentRepository.java
package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.BatchFeeAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchFeeAssignmentRepository {
    BatchFeeAssignment save(BatchFeeAssignment assignment);
    Optional<BatchFeeAssignment> findById(UUID id);
    List<BatchFeeAssignment> findByBatchId(UUID batchId);
    List<BatchFeeAssignment> findByCenterId(UUID centerId);
    void deleteById(UUID id);
}
