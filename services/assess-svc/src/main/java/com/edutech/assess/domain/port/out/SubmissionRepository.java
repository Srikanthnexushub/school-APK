// src/main/java/com/edutech/assess/domain/port/out/SubmissionRepository.java
package com.edutech.assess.domain.port.out;

import com.edutech.assess.domain.model.Submission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository {
    Submission save(Submission submission);
    Optional<Submission> findById(UUID id);
    List<Submission> findByExamIdAndStudentId(UUID examId, UUID studentId);
    List<Submission> findByStudentId(UUID studentId);
    long countByExamIdAndStudentId(UUID examId, UUID studentId);
}
