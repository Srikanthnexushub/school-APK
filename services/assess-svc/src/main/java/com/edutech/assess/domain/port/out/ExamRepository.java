// src/main/java/com/edutech/assess/domain/port/out/ExamRepository.java
package com.edutech.assess.domain.port.out;

import com.edutech.assess.domain.model.Exam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamRepository {
    Exam save(Exam exam);
    Optional<Exam> findById(UUID id);
    List<Exam> findByBatchId(UUID batchId);
    List<Exam> findByCenterId(UUID centerId);
}
