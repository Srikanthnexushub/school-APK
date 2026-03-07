// src/main/java/com/edutech/parent/infrastructure/persistence/StudentLinkPersistenceAdapter.java
package com.edutech.parent.infrastructure.persistence;

import com.edutech.parent.domain.model.StudentLink;
import com.edutech.parent.domain.port.out.StudentLinkRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class StudentLinkPersistenceAdapter implements StudentLinkRepository {

    private final SpringDataStudentLinkRepository repository;

    StudentLinkPersistenceAdapter(SpringDataStudentLinkRepository repository) {
        this.repository = repository;
    }

    @Override
    public StudentLink save(StudentLink link) {
        return repository.save(link);
    }

    @Override
    public Optional<StudentLink> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<StudentLink> findByParentId(UUID parentId) {
        return repository.findAllByParentId(parentId);
    }

    @Override
    public Optional<StudentLink> findByParentIdAndStudentId(UUID parentId, UUID studentId) {
        return repository.findByParentIdAndStudentId(parentId, studentId);
    }

    @Override
    public List<StudentLink> findActiveByStudentId(UUID studentId) {
        return repository.findActiveByStudentId(studentId);
    }
}
