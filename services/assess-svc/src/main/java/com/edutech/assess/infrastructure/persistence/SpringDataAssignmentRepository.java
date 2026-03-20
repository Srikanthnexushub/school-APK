// src/main/java/com/edutech/assess/infrastructure/persistence/SpringDataAssignmentRepository.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataAssignmentRepository extends JpaRepository<Assignment, UUID> {

    @Query("SELECT a FROM Assignment a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Assignment> findActiveById(@Param("id") UUID id);

    @Query("SELECT a FROM Assignment a WHERE a.batchId = :batchId AND a.deletedAt IS NULL ORDER BY a.createdAt DESC")
    List<Assignment> findByBatchIdActive(@Param("batchId") UUID batchId);

    @Query("SELECT a FROM Assignment a WHERE a.centerId = :centerId AND a.deletedAt IS NULL ORDER BY a.createdAt DESC")
    List<Assignment> findByCenterIdActive(@Param("centerId") UUID centerId);

    @Query("SELECT a FROM Assignment a WHERE a.batchId = :batchId AND a.status = 'PUBLISHED' AND a.deletedAt IS NULL ORDER BY a.createdAt DESC")
    List<Assignment> findPublishedByBatchId(@Param("batchId") UUID batchId);
}
