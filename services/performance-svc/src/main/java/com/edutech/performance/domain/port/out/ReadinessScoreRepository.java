package com.edutech.performance.domain.port.out;

import com.edutech.performance.domain.model.ReadinessScore;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReadinessScoreRepository {

    ReadinessScore save(ReadinessScore score);

    Optional<ReadinessScore> findLatestByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId);

    List<ReadinessScore> findByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId);

    Optional<ReadinessScore> findLatestByStudentId(UUID studentId);
}
