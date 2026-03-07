package com.edutech.examtracker.infrastructure.persistence;

import com.edutech.examtracker.domain.model.MockTestAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataMockTestAttemptRepository extends JpaRepository<MockTestAttempt, UUID> {

    @Query("SELECT a FROM MockTestAttempt a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<MockTestAttempt> findByIdActive(UUID id);

    @Query("SELECT a FROM MockTestAttempt a WHERE a.studentId = :studentId AND a.enrollmentId = :enrollmentId AND a.deletedAt IS NULL")
    List<MockTestAttempt> findByStudentIdAndEnrollmentIdActive(UUID studentId, UUID enrollmentId);

    @Query("SELECT a FROM MockTestAttempt a WHERE a.studentId = :studentId AND a.deletedAt IS NULL ORDER BY a.attemptDate DESC")
    List<MockTestAttempt> findTopByStudentId(UUID studentId, Pageable pageable);
}
