// src/main/java/com/edutech/assess/infrastructure/persistence/SubmissionPersistenceAdapter.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.Submission;
import com.edutech.assess.domain.port.out.SubmissionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class SubmissionPersistenceAdapter implements SubmissionRepository {

    private final SpringDataSubmissionRepository repository;

    SubmissionPersistenceAdapter(SpringDataSubmissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Submission save(Submission s) {
        return repository.save(s);
    }

    @Override
    public Optional<Submission> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<Submission> findByExamIdAndStudentId(UUID examId, UUID studentId) {
        return repository.findByExamIdAndStudentId(examId, studentId);
    }

    @Override
    public List<Submission> findByStudentId(UUID studentId) {
        return repository.findByStudentId(studentId);
    }

    @Override
    public long countByExamIdAndStudentId(UUID examId, UUID studentId) {
        return repository.countByExamIdAndStudentId(examId, studentId);
    }
}
