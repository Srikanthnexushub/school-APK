// src/main/java/com/edutech/assess/infrastructure/persistence/SpringDataExamRepository.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataExamRepository extends JpaRepository<Exam, UUID> {

    @Query("SELECT e FROM Exam e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Exam> findByIdActive(@Param("id") UUID id);

    @Query("SELECT e FROM Exam e WHERE e.batchId = :batchId AND e.deletedAt IS NULL ORDER BY e.createdAt DESC")
    List<Exam> findByBatchId(@Param("batchId") UUID batchId);

    @Query("SELECT e FROM Exam e WHERE e.centerId = :centerId AND e.deletedAt IS NULL ORDER BY e.createdAt DESC")
    List<Exam> findByCenterId(@Param("centerId") UUID centerId);
}
