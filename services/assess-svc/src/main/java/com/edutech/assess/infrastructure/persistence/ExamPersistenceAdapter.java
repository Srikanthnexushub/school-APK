// src/main/java/com/edutech/assess/infrastructure/persistence/ExamPersistenceAdapter.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.Exam;
import com.edutech.assess.domain.port.out.ExamRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ExamPersistenceAdapter implements ExamRepository {

    private final SpringDataExamRepository repository;

    ExamPersistenceAdapter(SpringDataExamRepository repository) {
        this.repository = repository;
    }

    @Override
    public Exam save(Exam exam) {
        return repository.save(exam);
    }

    @Override
    public Optional<Exam> findById(UUID id) {
        return repository.findByIdActive(id);
    }

    @Override
    public List<Exam> findByBatchId(UUID batchId) {
        return repository.findByBatchId(batchId);
    }

    @Override
    public List<Exam> findByCenterId(UUID centerId) {
        return repository.findByCenterId(centerId);
    }
}
