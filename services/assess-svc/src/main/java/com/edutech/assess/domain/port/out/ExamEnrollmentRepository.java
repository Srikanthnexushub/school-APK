// src/main/java/com/edutech/assess/domain/port/out/ExamEnrollmentRepository.java
package com.edutech.assess.domain.port.out;

import com.edutech.assess.domain.model.ExamEnrollment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamEnrollmentRepository {
    ExamEnrollment save(ExamEnrollment enrollment);
    Optional<ExamEnrollment> findById(UUID id);
    Optional<ExamEnrollment> findByExamIdAndStudentId(UUID examId, UUID studentId);
    List<ExamEnrollment> findByExamId(UUID examId);
    List<ExamEnrollment> findByStudentId(UUID studentId);
}
