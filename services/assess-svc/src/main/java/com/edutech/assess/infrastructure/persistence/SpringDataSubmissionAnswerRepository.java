// src/main/java/com/edutech/assess/infrastructure/persistence/SpringDataSubmissionAnswerRepository.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.SubmissionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SpringDataSubmissionAnswerRepository extends JpaRepository<SubmissionAnswer, UUID> {

    @Query("SELECT a FROM SubmissionAnswer a WHERE a.submissionId = :submissionId ORDER BY a.answeredAt ASC")
    List<SubmissionAnswer> findBySubmissionId(@Param("submissionId") UUID submissionId);
}
