package com.edutech.examtracker.infrastructure.persistence;

import com.edutech.examtracker.domain.model.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataStudySessionRepository extends JpaRepository<StudySession, UUID> {

    @Query("SELECT s FROM StudySession s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<StudySession> findByIdActive(UUID id);

    @Query("SELECT s FROM StudySession s WHERE s.studentId = :studentId AND s.enrollmentId = :enrollmentId AND s.deletedAt IS NULL")
    List<StudySession> findByStudentIdAndEnrollmentIdActive(UUID studentId, UUID enrollmentId);

    @Query("SELECT s FROM StudySession s WHERE s.studentId = :studentId AND s.sessionDate = :sessionDate AND s.deletedAt IS NULL")
    List<StudySession> findByStudentIdAndSessionDateActive(UUID studentId, LocalDate sessionDate);
}
