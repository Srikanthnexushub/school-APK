// src/main/java/com/edutech/center/domain/port/out/BatchRepository.java
package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.Batch;
import com.edutech.center.domain.model.BatchStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchRepository {
    Batch save(Batch batch);
    Optional<Batch> findById(UUID id);
    List<Batch> findByCenterId(UUID centerId);
    List<Batch> findByCenterIdAndStatus(UUID centerId, BatchStatus status);
    Optional<Batch> findByIdAndCenterId(UUID id, UUID centerId);
}
