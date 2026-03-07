package com.edutech.performance.infrastructure.persistence;

import com.edutech.performance.domain.model.WeakAreaRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SpringDataWeakAreaRepository extends JpaRepository<WeakAreaRecord, UUID> {

    @Query("SELECT w FROM WeakAreaRecord w WHERE w.studentId = :studentId AND w.enrollmentId = :enrollmentId AND w.deletedAt IS NULL")
    List<WeakAreaRecord> findByStudentIdAndEnrollmentId(@Param("studentId") UUID studentId,
                                                        @Param("enrollmentId") UUID enrollmentId);

    @Query("SELECT w FROM WeakAreaRecord w WHERE w.studentId = :studentId AND w.deletedAt IS NULL ORDER BY w.masteryPercent ASC LIMIT :limit")
    List<WeakAreaRecord> findByStudentIdOrderByMasteryAsc(@Param("studentId") UUID studentId,
                                                          @Param("limit") int limit);

    @Query("SELECT w FROM WeakAreaRecord w WHERE w.studentId = :studentId AND w.subject = :subject AND w.deletedAt IS NULL")
    List<WeakAreaRecord> findByStudentIdAndSubject(@Param("studentId") UUID studentId,
                                                   @Param("subject") String subject);
}
