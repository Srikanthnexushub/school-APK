package com.edutech.examtracker.infrastructure.persistence;

import com.edutech.examtracker.domain.model.SyllabusModule;
import com.edutech.examtracker.domain.port.out.SyllabusModuleRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SyllabusModulePersistenceAdapter implements SyllabusModuleRepository {

    private final SpringDataSyllabusModuleRepository jpa;

    public SyllabusModulePersistenceAdapter(SpringDataSyllabusModuleRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public SyllabusModule save(SyllabusModule module) {
        return jpa.save(module);
    }

    @Override
    public Optional<SyllabusModule> findById(UUID id) {
        return jpa.findByIdActive(id);
    }

    @Override
    public List<SyllabusModule> findByEnrollmentId(UUID enrollmentId) {
        return jpa.findByEnrollmentIdActive(enrollmentId);
    }

    @Override
    public List<SyllabusModule> findByStudentIdAndSubject(UUID studentId, String subject) {
        return jpa.findByStudentIdAndSubjectActive(studentId, subject);
    }
}
