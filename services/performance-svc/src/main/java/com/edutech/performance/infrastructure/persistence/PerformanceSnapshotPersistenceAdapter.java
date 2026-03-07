package com.edutech.performance.infrastructure.persistence;

import com.edutech.performance.domain.model.PerformanceSnapshot;
import com.edutech.performance.domain.port.out.PerformanceSnapshotRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PerformanceSnapshotPersistenceAdapter implements PerformanceSnapshotRepository {

    private final SpringDataPerformanceSnapshotRepository springData;

    public PerformanceSnapshotPersistenceAdapter(SpringDataPerformanceSnapshotRepository springData) {
        this.springData = springData;
    }

    @Override
    public PerformanceSnapshot save(PerformanceSnapshot snapshot) {
        return springData.save(snapshot);
    }

    @Override
    public Optional<PerformanceSnapshot> findLatestByStudentId(UUID studentId) {
        return springData.findLatestByStudentId(studentId);
    }

    @Override
    public List<PerformanceSnapshot> findByStudentIdAndSnapshotAtBetween(UUID studentId, Instant from, Instant to) {
        return springData.findByStudentIdAndSnapshotAtBetween(studentId, from, to);
    }
}
