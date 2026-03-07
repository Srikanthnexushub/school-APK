package com.edutech.examtracker.infrastructure.persistence;

import com.edutech.examtracker.domain.model.SyllabusModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataSyllabusModuleRepository extends JpaRepository<SyllabusModule, UUID> {

    @Query("SELECT m FROM SyllabusModule m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<SyllabusModule> findByIdActive(UUID id);

    @Query("SELECT m FROM SyllabusModule m WHERE m.enrollmentId = :enrollmentId AND m.deletedAt IS NULL")
    List<SyllabusModule> findByEnrollmentIdActive(UUID enrollmentId);

    @Query("SELECT m FROM SyllabusModule m WHERE m.studentId = :studentId AND m.subject = :subject AND m.deletedAt IS NULL")
    List<SyllabusModule> findByStudentIdAndSubjectActive(UUID studentId, String subject);
}
