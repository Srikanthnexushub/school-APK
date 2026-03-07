// src/main/java/com/edutech/assess/infrastructure/persistence/GradePersistenceAdapter.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.Grade;
import com.edutech.assess.domain.port.out.GradeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class GradePersistenceAdapter implements GradeRepository {

    private final SpringDataGradeRepository repository;

    GradePersistenceAdapter(SpringDataGradeRepository repository) {
        this.repository = repository;
    }

    @Override
    public Grade save(Grade g) {
        return repository.save(g);
    }

    @Override
    public Optional<Grade> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Grade> findBySubmissionId(UUID submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Override
    public List<Grade> findByExamId(UUID examId) {
        return repository.findByExamId(examId);
    }

    @Override
    public List<Grade> findByStudentId(UUID studentId) {
        return repository.findByStudentId(studentId);
    }
}
