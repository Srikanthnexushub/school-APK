// src/main/java/com/edutech/assess/domain/port/out/AssignmentRepository.java
package com.edutech.assess.domain.port.out;

import com.edutech.assess.domain.model.Assignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository {
    Optional<Assignment> findById(UUID id);
    Optional<Assignment> findActiveById(UUID id);
    List<Assignment> findByBatchIdActive(UUID batchId);
    List<Assignment> findByCenterIdActive(UUID centerId);
    List<Assignment> findPublishedByBatchId(UUID batchId);
    Assignment save(Assignment assignment);
    void delete(Assignment assignment);
}
