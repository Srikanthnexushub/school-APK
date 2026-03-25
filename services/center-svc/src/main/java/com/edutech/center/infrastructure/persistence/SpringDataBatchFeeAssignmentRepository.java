// src/main/java/com/edutech/center/infrastructure/persistence/SpringDataBatchFeeAssignmentRepository.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.BatchFeeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataBatchFeeAssignmentRepository extends JpaRepository<BatchFeeAssignment, UUID> {
    List<BatchFeeAssignment> findByBatchId(UUID batchId);
    List<BatchFeeAssignment> findByCenterId(UUID centerId);
}
