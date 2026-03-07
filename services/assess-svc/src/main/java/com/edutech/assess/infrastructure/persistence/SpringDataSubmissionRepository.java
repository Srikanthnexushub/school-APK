// src/main/java/com/edutech/assess/infrastructure/persistence/SpringDataSubmissionRepository.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SpringDataSubmissionRepository extends JpaRepository<Submission, UUID> {

    @Query("SELECT s FROM Submission s WHERE s.examId = :examId AND s.studentId = :studentId ORDER BY s.attemptNumber DESC")
    List<Submission> findByExamIdAndStudentId(@Param("examId") UUID examId, @Param("studentId") UUID studentId);

    @Query("SELECT s FROM Submission s WHERE s.studentId = :studentId ORDER BY s.createdAt DESC")
    List<Submission> findByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.examId = :examId AND s.studentId = :studentId")
    long countByExamIdAndStudentId(@Param("examId") UUID examId, @Param("studentId") UUID studentId);
}
