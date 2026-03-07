// src/main/java/com/edutech/assess/infrastructure/persistence/SpringDataGradeRepository.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataGradeRepository extends JpaRepository<Grade, UUID> {

    @Query("SELECT g FROM Grade g WHERE g.submissionId = :submissionId")
    Optional<Grade> findBySubmissionId(@Param("submissionId") UUID submissionId);

    @Query("SELECT g FROM Grade g WHERE g.examId = :examId ORDER BY g.percentage DESC")
    List<Grade> findByExamId(@Param("examId") UUID examId);

    @Query("SELECT g FROM Grade g WHERE g.studentId = :studentId ORDER BY g.createdAt DESC")
    List<Grade> findByStudentId(@Param("studentId") UUID studentId);
}
