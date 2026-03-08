// src/main/java/com/edutech/assess/infrastructure/persistence/SpringDataExamEnrollmentRepository.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.ExamEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataExamEnrollmentRepository extends JpaRepository<ExamEnrollment, UUID> {

    @Query("SELECT e FROM ExamEnrollment e WHERE e.examId = :examId AND e.studentId = :studentId")
    Optional<ExamEnrollment> findByExamIdAndStudentId(@Param("examId") UUID examId, @Param("studentId") UUID studentId);

    @Query("SELECT e FROM ExamEnrollment e WHERE e.examId = :examId ORDER BY e.enrolledAt DESC")
    List<ExamEnrollment> findByExamId(@Param("examId") UUID examId);

    @Query("SELECT e FROM ExamEnrollment e WHERE e.studentId = :studentId")
    List<ExamEnrollment> findByStudentId(@Param("studentId") UUID studentId);
}
