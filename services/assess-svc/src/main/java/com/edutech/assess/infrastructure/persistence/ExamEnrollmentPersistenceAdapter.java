// src/main/java/com/edutech/assess/infrastructure/persistence/ExamEnrollmentPersistenceAdapter.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.ExamEnrollment;
import com.edutech.assess.domain.port.out.ExamEnrollmentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ExamEnrollmentPersistenceAdapter implements ExamEnrollmentRepository {

    private final SpringDataExamEnrollmentRepository repository;

    ExamEnrollmentPersistenceAdapter(SpringDataExamEnrollmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExamEnrollment save(ExamEnrollment e) {
        return repository.save(e);
    }

    @Override
    public Optional<ExamEnrollment> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<ExamEnrollment> findByExamIdAndStudentId(UUID examId, UUID studentId) {
        return repository.findByExamIdAndStudentId(examId, studentId);
    }

    @Override
    public List<ExamEnrollment> findByExamId(UUID examId) {
        return repository.findByExamId(examId);
    }
}
