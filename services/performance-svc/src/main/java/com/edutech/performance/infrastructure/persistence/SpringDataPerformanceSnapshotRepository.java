package com.edutech.performance.infrastructure.persistence;

import com.edutech.performance.domain.model.PerformanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataPerformanceSnapshotRepository extends JpaRepository<PerformanceSnapshot, UUID> {

    @Query(value = "SELECT * FROM performance_schema.performance_snapshots WHERE student_id = :studentId ORDER BY snapshot_at DESC LIMIT 1",
           nativeQuery = true)
    Optional<PerformanceSnapshot> findLatestByStudentId(@Param("studentId") UUID studentId);

    List<PerformanceSnapshot> findByStudentIdAndSnapshotAtBetween(UUID studentId, Instant from, Instant to);
}
