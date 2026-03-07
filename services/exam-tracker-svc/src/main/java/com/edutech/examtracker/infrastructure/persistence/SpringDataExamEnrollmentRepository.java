package com.edutech.examtracker.infrastructure.persistence;

import com.edutech.examtracker.domain.model.ExamCode;
import com.edutech.examtracker.domain.model.ExamEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataExamEnrollmentRepository extends JpaRepository<ExamEnrollment, UUID> {

    @Query("SELECT e FROM ExamEnrollment e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<ExamEnrollment> findByIdActive(UUID id);

    @Query("SELECT e FROM ExamEnrollment e WHERE e.studentId = :studentId AND e.deletedAt IS NULL")
    List<ExamEnrollment> findByStudentIdActive(UUID studentId);

    @Query("SELECT e FROM ExamEnrollment e WHERE e.studentId = :studentId AND e.examCode = :examCode AND e.deletedAt IS NULL")
    Optional<ExamEnrollment> findByStudentIdAndExamCodeActive(UUID studentId, ExamCode examCode);
}
