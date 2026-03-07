package com.edutech.student.infrastructure.persistence;

import com.edutech.student.domain.model.TargetExam;
import com.edutech.student.domain.port.out.TargetExamRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TargetExamPersistenceAdapter implements TargetExamRepository {

    private final SpringDataTargetExamRepository repository;

    TargetExamPersistenceAdapter(SpringDataTargetExamRepository repository) {
        this.repository = repository;
    }

    @Override
    public TargetExam save(TargetExam exam) {
        return repository.save(exam);
    }

    @Override
    public Optional<TargetExam> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<TargetExam> findByStudentId(UUID studentId) {
        return repository.findByStudentIdAndDeletedAtIsNull(studentId);
    }

    @Override
    public void deleteById(UUID id) {
        repository.findById(id).ifPresent(exam -> {
            exam.softDelete();
            repository.save(exam);
        });
    }
}
