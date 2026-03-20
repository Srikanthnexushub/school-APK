// src/main/java/com/edutech/assess/application/service/AssignmentService.java
package com.edutech.assess.application.service;

import com.edutech.assess.application.dto.AssignmentResponse;
import com.edutech.assess.application.dto.AssignmentSubmissionResponse;
import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.CreateAssignmentRequest;
import com.edutech.assess.application.dto.GradeSubmissionRequest;
import com.edutech.assess.application.dto.SubmitAssignmentRequest;
import com.edutech.assess.application.dto.UpdateAssignmentRequest;
import com.edutech.assess.application.exception.AssignmentAccessDeniedException;
import com.edutech.assess.application.exception.AssignmentNotFoundException;
import com.edutech.assess.application.exception.AssignmentSubmissionNotFoundException;
import com.edutech.assess.domain.model.Assignment;
import com.edutech.assess.domain.model.AssignmentStatus;
import com.edutech.assess.domain.model.AssignmentSubmission;
import com.edutech.assess.domain.model.Role;
import com.edutech.assess.domain.port.out.AssignmentRepository;
import com.edutech.assess.domain.port.out.AssignmentSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;

    public AssignmentService(AssignmentRepository assignmentRepository,
                              AssignmentSubmissionRepository assignmentSubmissionRepository) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
    }

    public AssignmentResponse createAssignment(CreateAssignmentRequest request, AuthPrincipal principal) {
        if (!principal.isSuperAdmin() && !principal.isTeacher() && !principal.isCenterAdmin()) {
            throw new AssignmentAccessDeniedException();
        }
        if (!principal.isSuperAdmin() && !principal.belongsToCenter(request.centerId())) {
            throw new AssignmentAccessDeniedException();
        }
        Assignment assignment = Assignment.create(
                request.batchId(),
                request.centerId(),
                principal.userId(),
                request.title(),
                request.description(),
                request.type(),
                request.dueDate(),
                request.totalMarks(),
                request.passingMarks(),
                request.instructions(),
                request.attachmentUrl()
        );
        Assignment saved = assignmentRepository.save(assignment);
        log.info("Assignment created: id={} batchId={}", saved.getId(), saved.getBatchId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignment(UUID id, AuthPrincipal principal) {
        Assignment assignment = assignmentRepository.findActiveById(id)
                .orElseThrow(() -> new AssignmentNotFoundException(id));
        if (principal.isSuperAdmin()) {
            // always allowed
        } else if (principal.isCenterAdmin() || principal.isTeacher()) {
            if (!principal.belongsToCenter(assignment.getCenterId())) {
                throw new AssignmentAccessDeniedException();
            }
        } else if (principal.role() == Role.STUDENT) {
            if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
                throw new AssignmentAccessDeniedException();
            }
        } else if (principal.role() == Role.PARENT) {
            if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
                throw new AssignmentAccessDeniedException();
            }
        } else {
            throw new AssignmentAccessDeniedException();
        }
        return toResponse(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listByBatch(UUID batchId, AuthPrincipal principal) {
        if (principal.role() == Role.STUDENT || principal.role() == Role.PARENT) {
            return assignmentRepository.findPublishedByBatchId(batchId).stream()
                    .map(this::toResponse).toList();
        }
        return assignmentRepository.findByBatchIdActive(batchId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listByCenter(UUID centerId, AuthPrincipal principal) {
        if (!principal.isSuperAdmin() && !principal.belongsToCenter(centerId)) {
            throw new AssignmentAccessDeniedException();
        }
        return assignmentRepository.findByCenterIdActive(centerId).stream()
                .map(this::toResponse).toList();
    }

    public AssignmentResponse updateAssignment(UUID id, UpdateAssignmentRequest request, AuthPrincipal principal) {
        Assignment assignment = assignmentRepository.findActiveById(id)
                .orElseThrow(() -> new AssignmentNotFoundException(id));
        if (!principal.isSuperAdmin()
                && !principal.belongsToCenter(assignment.getCenterId())
                && !assignment.isOwnedBy(principal.userId())) {
            throw new AssignmentAccessDeniedException();
        }
        assignment.updateDetails(
                request.title(),
                request.description(),
                request.instructions(),
                request.attachmentUrl(),
                request.dueDate(),
                request.totalMarks(),
                request.passingMarks()
        );
        return toResponse(assignmentRepository.save(assignment));
    }

    public AssignmentResponse publishAssignment(UUID id, AuthPrincipal principal) {
        Assignment assignment = getAndCheckManageAccess(id, principal);
        assignment.publish();
        Assignment saved = assignmentRepository.save(assignment);
        log.info("Assignment published: id={} batchId={}", saved.getId(), saved.getBatchId());
        return toResponse(saved);
    }

    public AssignmentResponse closeAssignment(UUID id, AuthPrincipal principal) {
        Assignment assignment = getAndCheckManageAccess(id, principal);
        assignment.close();
        return toResponse(assignmentRepository.save(assignment));
    }

    public void deleteAssignment(UUID id, AuthPrincipal principal) {
        Assignment assignment = getAndCheckManageAccess(id, principal);
        assignment.softDelete();
        assignmentRepository.save(assignment);
    }

    public AssignmentSubmissionResponse submitAssignment(UUID assignmentId, SubmitAssignmentRequest request,
                                                          AuthPrincipal principal) {
        if (principal.role() != Role.STUDENT) {
            throw new AssignmentAccessDeniedException();
        }
        Assignment assignment = assignmentRepository.findActiveById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new AssignmentAccessDeniedException();
        }
        AssignmentSubmission submission = assignmentSubmissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, principal.userId())
                .orElseGet(() -> buildNewSubmission(assignmentId, principal.userId()));

        boolean isLate = assignment.getDueDate() != null && Instant.now().isAfter(assignment.getDueDate());
        if (isLate) {
            submission.submitLate(request.textResponse());
        } else {
            submission.submit(request.textResponse());
        }
        return toSubmissionResponse(assignmentSubmissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> listSubmissions(UUID assignmentId, AuthPrincipal principal) {
        Assignment assignment = assignmentRepository.findActiveById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));

        if (principal.role() == Role.STUDENT) {
            return assignmentSubmissionRepository
                    .findByAssignmentIdAndStudentId(assignmentId, principal.userId())
                    .stream().map(this::toSubmissionResponse).toList();
        }
        if (principal.role() == Role.PARENT) {
            throw new AssignmentAccessDeniedException();
        }
        if (!principal.isSuperAdmin() && !principal.belongsToCenter(assignment.getCenterId())) {
            throw new AssignmentAccessDeniedException();
        }
        return assignmentSubmissionRepository.findByAssignmentId(assignmentId).stream()
                .map(this::toSubmissionResponse).toList();
    }

    public AssignmentSubmissionResponse gradeSubmission(UUID assignmentId, UUID submissionId,
                                                         GradeSubmissionRequest request,
                                                         AuthPrincipal principal) {
        Assignment assignment = assignmentRepository.findActiveById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));
        if (!principal.isSuperAdmin() && !principal.belongsToCenter(assignment.getCenterId())) {
            throw new AssignmentAccessDeniedException();
        }
        AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new AssignmentSubmissionNotFoundException(submissionId));
        submission.grade(request.score(), request.feedback());
        return toSubmissionResponse(assignmentSubmissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> listStudentSubmissions(UUID studentId, AuthPrincipal principal) {
        if (principal.role() == Role.STUDENT && !principal.userId().equals(studentId)) {
            throw new AssignmentAccessDeniedException();
        }
        if (principal.role() == Role.GUEST) {
            throw new AssignmentAccessDeniedException();
        }
        return assignmentSubmissionRepository.findByStudentId(studentId).stream()
                .map(this::toSubmissionResponse).toList();
    }

    private Assignment getAndCheckManageAccess(UUID id, AuthPrincipal principal) {
        Assignment assignment = assignmentRepository.findActiveById(id)
                .orElseThrow(() -> new AssignmentNotFoundException(id));
        if (!principal.isSuperAdmin()
                && !principal.belongsToCenter(assignment.getCenterId())
                && !assignment.isOwnedBy(principal.userId())) {
            throw new AssignmentAccessDeniedException();
        }
        return assignment;
    }

    private AssignmentSubmission buildNewSubmission(UUID assignmentId, UUID studentId) {
        return AssignmentSubmission.create(assignmentId, studentId);
    }

    private AssignmentResponse toResponse(Assignment a) {
        return new AssignmentResponse(
                a.getId(),
                a.getBatchId(),
                a.getCenterId(),
                a.getCreatedByUserId(),
                a.getTitle(),
                a.getDescription(),
                a.getType(),
                a.getDueDate(),
                a.getTotalMarks(),
                a.getPassingMarks(),
                a.getInstructions(),
                a.getAttachmentUrl(),
                a.getStatus(),
                a.getCreatedAt(),
                -1
        );
    }

    private AssignmentSubmissionResponse toSubmissionResponse(AssignmentSubmission s) {
        return new AssignmentSubmissionResponse(
                s.getId(),
                s.getAssignmentId(),
                s.getStudentId(),
                s.getTextResponse(),
                s.getScore(),
                s.getFeedback(),
                s.getStatus(),
                s.getSubmittedAt(),
                s.getGradedAt(),
                s.getCreatedAt()
        );
    }
}
