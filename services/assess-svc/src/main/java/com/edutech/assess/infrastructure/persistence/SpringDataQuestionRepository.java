// src/main/java/com/edutech/assess/infrastructure/persistence/SpringDataQuestionRepository.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataQuestionRepository extends JpaRepository<Question, UUID> {

    @Query("SELECT q FROM Question q WHERE q.id = :id AND q.deletedAt IS NULL")
    Optional<Question> findByIdActive(@Param("id") UUID id);

    @Query("SELECT q FROM Question q WHERE q.examId = :examId AND q.deletedAt IS NULL ORDER BY q.createdAt ASC")
    List<Question> findByExamId(@Param("examId") UUID examId);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.examId = :examId AND q.deletedAt IS NULL")
    int countByExamId(@Param("examId") UUID examId);
}
