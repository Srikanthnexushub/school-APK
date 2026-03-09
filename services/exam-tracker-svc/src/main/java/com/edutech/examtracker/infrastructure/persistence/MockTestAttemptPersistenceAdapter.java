package com.edutech.examtracker.infrastructure.persistence;

import com.edutech.examtracker.domain.model.MockTestAttempt;
import com.edutech.examtracker.domain.port.out.MockTestAttemptRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MockTestAttemptPersistenceAdapter implements MockTestAttemptRepository {

    private final SpringDataMockTestAttemptRepository jpa;

    public MockTestAttemptPersistenceAdapter(SpringDataMockTestAttemptRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public MockTestAttempt save(MockTestAttempt attempt) {
        return jpa.save(attempt);
    }

    @Override
    public Optional<MockTestAttempt> findById(UUID id) {
        return jpa.findByIdActive(id);
    }

    @Override
    public List<MockTestAttempt> findByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId) {
        return jpa.findByStudentIdAndEnrollmentIdActive(studentId, enrollmentId);
    }

    @Override
    public List<MockTestAttempt> findTopNByStudentId(UUID studentId, int n) {
        return jpa.findTopByStudentId(studentId, PageRequest.of(0, n));
    }

    @Override
    public List<MockTestAttempt> findByStudentId(UUID studentId) {
        return jpa.findByStudentIdActive(studentId);
    }
}
