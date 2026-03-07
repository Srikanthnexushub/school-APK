package com.edutech.examtracker.domain.port.out;

import com.edutech.examtracker.domain.model.MockTestAttempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MockTestAttemptRepository {

    MockTestAttempt save(MockTestAttempt attempt);

    Optional<MockTestAttempt> findById(UUID id);

    List<MockTestAttempt> findByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId);

    List<MockTestAttempt> findTopNByStudentId(UUID studentId, int n);
}
