// src/main/java/com/edutech/center/domain/port/out/FeeStructureRepository.java
package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.FeeStructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeeStructureRepository {
    FeeStructure save(FeeStructure feeStructure);
    Optional<FeeStructure> findById(UUID id);
    List<FeeStructure> findByCenterId(UUID centerId);
}
