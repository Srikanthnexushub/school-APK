// src/main/java/com/edutech/assess/infrastructure/persistence/AssignmentSubmissionPersistenceAdapter.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.AssignmentSubmission;
import com.edutech.assess.domain.port.out.AssignmentSubmissionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class AssignmentSubmissionPersistenceAdapter implements AssignmentSubmissionRepository {

    private final SpringDataAssignmentSubmissionRepository repository;

    AssignmentSubmissionPersistenceAdapter(SpringDataAssignmentSubmissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AssignmentSubmission> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId) {
        return repository.findByAssignmentIdAndStudentId(assignmentId, studentId);
    }

    @Override
    public List<AssignmentSubmission> findByAssignmentId(UUID assignmentId) {
        return repository.findByAssignmentId(assignmentId);
    }

    @Override
    public List<AssignmentSubmission> findByStudentId(UUID studentId) {
        return repository.findByStudentId(studentId);
    }

    @Override
    public AssignmentSubmission save(AssignmentSubmission submission) {
        return repository.save(submission);
    }
}
