package com.edutech.student.infrastructure.persistence;

import com.edutech.student.domain.model.StudentProfile;
import com.edutech.student.domain.port.out.StudentProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class StudentProfilePersistenceAdapter implements StudentProfileRepository {

    private final SpringDataStudentProfileRepository repository;

    StudentProfilePersistenceAdapter(SpringDataStudentProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public StudentProfile save(StudentProfile profile) {
        return repository.save(profile);
    }

    @Override
    public Optional<StudentProfile> findById(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<StudentProfile> findByUserId(UUID userId) {
        return repository.findByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public Optional<StudentProfile> findByEmail(String email) {
        return repository.findByEmailAndDeletedAtIsNull(email);
    }

    @Override
    public Optional<StudentProfile> findByParentLinkCode(String code) {
        return repository.findByParentLinkCodeAndDeletedAtIsNull(code);
    }
}
