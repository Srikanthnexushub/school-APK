// src/main/java/com/edutech/assess/domain/port/out/GradeRepository.java
package com.edutech.assess.domain.port.out;

import com.edutech.assess.domain.model.Grade;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeRepository {
    Grade save(Grade grade);
    Optional<Grade> findById(UUID id);
    Optional<Grade> findBySubmissionId(UUID submissionId);
    List<Grade> findByExamId(UUID examId);
    List<Grade> findByStudentId(UUID studentId);
}
