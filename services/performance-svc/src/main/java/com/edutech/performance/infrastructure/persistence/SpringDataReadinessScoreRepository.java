package com.edutech.performance.infrastructure.persistence;

import com.edutech.performance.domain.model.ReadinessScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataReadinessScoreRepository extends JpaRepository<ReadinessScore, UUID> {

    @Query("SELECT r FROM ReadinessScore r WHERE r.studentId = :studentId AND r.enrollmentId = :enrollmentId AND r.deletedAt IS NULL ORDER BY r.computedAt DESC LIMIT 1")
    Optional<ReadinessScore> findLatestByStudentIdAndEnrollmentId(@Param("studentId") UUID studentId,
                                                                   @Param("enrollmentId") UUID enrollmentId);

    @Query("SELECT r FROM ReadinessScore r WHERE r.studentId = :studentId AND r.enrollmentId = :enrollmentId AND r.deletedAt IS NULL ORDER BY r.computedAt DESC")
    List<ReadinessScore> findByStudentIdAndEnrollmentId(@Param("studentId") UUID studentId,
                                                         @Param("enrollmentId") UUID enrollmentId);
}
