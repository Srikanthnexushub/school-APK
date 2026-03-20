// src/main/java/com/edutech/assess/domain/port/out/AssignmentSubmissionRepository.java
package com.edutech.assess.domain.port.out;

import com.edutech.assess.domain.model.AssignmentSubmission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentSubmissionRepository {
    Optional<AssignmentSubmission> findById(UUID id);
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);
    List<AssignmentSubmission> findByAssignmentId(UUID assignmentId);
    List<AssignmentSubmission> findByStudentId(UUID studentId);
    AssignmentSubmission save(AssignmentSubmission submission);
}
