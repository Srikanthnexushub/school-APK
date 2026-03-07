// src/main/java/com/edutech/center/infrastructure/persistence/SpringDataFeeStructureRepository.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface SpringDataFeeStructureRepository extends JpaRepository<FeeStructure, UUID> {

    @Query("SELECT f FROM FeeStructure f WHERE f.centerId = :centerId AND f.status <> 'ARCHIVED'")
    List<FeeStructure> findActiveByCenterId(UUID centerId);
}
