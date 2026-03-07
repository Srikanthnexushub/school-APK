package com.edutech.performance.domain.port.out;

import com.edutech.performance.domain.model.PerformanceSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PerformanceSnapshotRepository {

    PerformanceSnapshot save(PerformanceSnapshot snapshot);

    Optional<PerformanceSnapshot> findLatestByStudentId(UUID studentId);

    List<PerformanceSnapshot> findByStudentIdAndSnapshotAtBetween(UUID studentId, Instant from, Instant to);
}
