package com.edutech.performance.infrastructure.persistence;

import com.edutech.performance.domain.model.ReadinessScore;
import com.edutech.performance.domain.port.out.ReadinessScoreRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ReadinessScorePersistenceAdapter implements ReadinessScoreRepository {

    private final SpringDataReadinessScoreRepository springData;

    public ReadinessScorePersistenceAdapter(SpringDataReadinessScoreRepository springData) {
        this.springData = springData;
    }

    @Override
    public ReadinessScore save(ReadinessScore score) {
        return springData.save(score);
    }

    @Override
    public Optional<ReadinessScore> findLatestByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId) {
        return springData.findLatestByStudentIdAndEnrollmentId(studentId, enrollmentId);
    }

    @Override
    public List<ReadinessScore> findByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId) {
        return springData.findByStudentIdAndEnrollmentId(studentId, enrollmentId);
    }

    @Override
    public Optional<ReadinessScore> findLatestByStudentId(UUID studentId) {
        return springData.findLatestByStudentId(studentId);
    }
}
