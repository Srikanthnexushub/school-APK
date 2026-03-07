package com.edutech.student.infrastructure.persistence;

import com.edutech.student.domain.model.AcademicRecord;
import com.edutech.student.domain.port.out.AcademicRecordRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AcademicRecordPersistenceAdapter implements AcademicRecordRepository {

    private final SpringDataAcademicRecordRepository repository;

    AcademicRecordPersistenceAdapter(SpringDataAcademicRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public AcademicRecord save(AcademicRecord record) {
        return repository.save(record);
    }

    @Override
    public Optional<AcademicRecord> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<AcademicRecord> findByStudentId(UUID studentId) {
        return repository.findByStudentIdAndDeletedAtIsNull(studentId);
    }
}
