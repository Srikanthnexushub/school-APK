// src/main/java/com/edutech/center/infrastructure/persistence/SpringDataBatchRepository.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.Batch;
import com.edutech.center.domain.model.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataBatchRepository extends JpaRepository<Batch, UUID> {

    @Query("SELECT b FROM Batch b WHERE b.centerId = :centerId AND b.deletedAt IS NULL")
    List<Batch> findByCenterIdActive(UUID centerId);

    @Query("SELECT b FROM Batch b WHERE b.centerId = :centerId AND b.status = :status AND b.deletedAt IS NULL")
    List<Batch> findByCenterIdAndStatusActive(UUID centerId, BatchStatus status);

    @Query("SELECT b FROM Batch b WHERE b.id = :id AND b.centerId = :centerId AND b.deletedAt IS NULL")
    Optional<Batch> findByIdAndCenterIdActive(UUID id, UUID centerId);

    // ── Audit stats ────────────────────────────────────────────────────────────
    @Query("SELECT COUNT(b) FROM Batch b WHERE b.deletedAt IS NULL")
    long countAllActive();

    @Query("SELECT COUNT(b) FROM Batch b WHERE b.status = com.edutech.center.domain.model.BatchStatus.ACTIVE AND b.deletedAt IS NULL")
    long countByStatusActive();

    @Query("SELECT COUNT(b) FROM Batch b WHERE b.status = com.edutech.center.domain.model.BatchStatus.UPCOMING AND b.deletedAt IS NULL")
    long countByStatusUpcoming();

    @Query("SELECT COUNT(b) FROM Batch b WHERE b.status = com.edutech.center.domain.model.BatchStatus.COMPLETED AND b.deletedAt IS NULL")
    long countByStatusCompleted();

    @Query("SELECT SUM(b.enrolledCount) FROM Batch b WHERE b.deletedAt IS NULL")
    Long sumEnrolledCount();

    @Query("SELECT AVG(CAST(b.enrolledCount AS double) / b.maxStudents) FROM Batch b WHERE b.maxStudents > 0 AND b.deletedAt IS NULL")
    Double avgFillRate();
}
