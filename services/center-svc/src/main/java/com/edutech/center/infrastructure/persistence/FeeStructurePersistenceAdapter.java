// src/main/java/com/edutech/center/infrastructure/persistence/FeeStructurePersistenceAdapter.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.FeeStructure;
import com.edutech.center.domain.port.out.FeeStructureRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FeeStructurePersistenceAdapter implements FeeStructureRepository {

    private final SpringDataFeeStructureRepository jpa;

    public FeeStructurePersistenceAdapter(SpringDataFeeStructureRepository jpa) { this.jpa = jpa; }

    @Override public FeeStructure save(FeeStructure f) { return jpa.save(f); }
    @Override public Optional<FeeStructure> findById(UUID id) { return jpa.findById(id); }
    @Override public List<FeeStructure> findByCenterId(UUID centerId) { return jpa.findActiveByCenterId(centerId); }
}
