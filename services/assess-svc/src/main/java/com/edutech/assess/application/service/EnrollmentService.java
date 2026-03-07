// src/main/java/com/edutech/assess/application/service/EnrollmentService.java
package com.edutech.assess.application.service;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.EnrollStudentRequest;
import com.edutech.assess.application.dto.EnrollmentResponse;
import com.edutech.assess.application.exception.AssessAccessDeniedException;
import com.edutech.assess.application.exception.DuplicateEnrollmentException;
import com.edutech.assess.application.exception.ExamNotFoundException;
import com.edutech.assess.application.exception.ExamNotPublishedException;
import com.edutech.assess.domain.model.EnrollmentStatus;
import com.edutech.assess.domain.model.Exam;
import com.edutech.assess.domain.model.ExamEnrollment;
import com.edutech.assess.domain.model.ExamStatus;
import com.edutech.assess.domain.port.in.EnrollStudentUseCase;
import com.edutech.assess.domain.port.out.ExamEnrollmentRepository;
import com.edutech.assess.domain.port.out.ExamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EnrollmentService implements EnrollStudentUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final ExamEnrollmentRepository enrollmentRepository;
    private final ExamRepository examRepository;

    public EnrollmentService(ExamEnrollmentRepository enrollmentRepository,
                              ExamRepository examRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.examRepository = examRepository;
    }

    @Override
    public EnrollmentResponse enrollStudent(UUID examId, EnrollStudentRequest request, AuthPrincipal principal) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new ExamNotPublishedException(examId);
        }
        if (!principal.belongsToCenter(exam.getCenterId()) && !principal.isSuperAdmin()) {
            throw new AssessAccessDeniedException();
        }
        Optional<ExamEnrollment> existing =
                enrollmentRepository.findByExamIdAndStudentId(examId, request.studentId());
        if (existing.isPresent() && existing.get().getStatus() == EnrollmentStatus.ENROLLED) {
            throw new DuplicateEnrollmentException(request.studentId(), examId);
        }
        ExamEnrollment enrollment = ExamEnrollment.create(examId, request.studentId());
        return toResponse(enrollmentRepository.save(enrollment));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listEnrollments(UUID examId, AuthPrincipal principal) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        if (!principal.belongsToCenter(exam.getCenterId())) {
            throw new AssessAccessDeniedException();
        }
        return enrollmentRepository.findByExamId(examId).stream()
                .map(this::toResponse)
                .toList();
    }

    private EnrollmentResponse toResponse(ExamEnrollment e) {
        return new EnrollmentResponse(
                e.getId(), e.getExamId(), e.getStudentId(), e.getStatus(), e.getEnrolledAt()
        );
    }
}
