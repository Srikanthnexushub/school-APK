// src/main/java/com/edutech/assess/application/service/GradeService.java
package com.edutech.assess.application.service;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.GradeResponse;
import com.edutech.assess.application.exception.AssessAccessDeniedException;
import com.edutech.assess.application.exception.ExamNotFoundException;
import com.edutech.assess.domain.model.Exam;
import com.edutech.assess.domain.model.Grade;
import com.edutech.assess.domain.port.out.ExamRepository;
import com.edutech.assess.domain.port.out.GradeRepository;
import com.edutech.assess.domain.port.out.SubmissionRepository;
import com.edutech.assess.domain.port.out.TeacherCenterValidationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GradeService {

    private static final Logger log = LoggerFactory.getLogger(GradeService.class);

    private final GradeRepository gradeRepository;
    private final SubmissionRepository submissionRepository;
    private final ExamRepository examRepository;
    private final TeacherCenterValidationPort teacherCenterValidation;

    public GradeService(GradeRepository gradeRepository,
                        SubmissionRepository submissionRepository,
                        ExamRepository examRepository,
                        TeacherCenterValidationPort teacherCenterValidation) {
        this.gradeRepository = gradeRepository;
        this.submissionRepository = submissionRepository;
        this.examRepository = examRepository;
        this.teacherCenterValidation = teacherCenterValidation;
    }

    public GradeResponse getGradeBySubmission(UUID submissionId, AuthPrincipal principal) {
        Grade grade = gradeRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new RuntimeException("Grade not found"));
        if (!principal.userId().equals(grade.getStudentId())
                && !principal.isSuperAdmin()
                && !principal.belongsToCenter(grade.getCenterId())) {
            throw new AssessAccessDeniedException();
        }
        return toResponse(grade);
    }

    public List<GradeResponse> listGradesByExam(UUID examId, AuthPrincipal principal) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));

        if (principal.isSuperAdmin() || principal.isInstitutionAdmin()) {
            // Platform-level admins can access any exam grades
        } else if (principal.isTeacher()) {
            // centerId in JWT may be null until re-login after approval — validate via DB (center-svc).
            boolean authorized = teacherCenterValidation.isTeacherAtCenter(
                    principal.userId(), exam.getCenterId());
            if (!authorized) {
                log.warn("TEACHER access denied to exam grades: teacherUserId={} examCenterId={}",
                        principal.userId(), exam.getCenterId());
                throw new AssessAccessDeniedException();
            }
        } else if (!principal.belongsToCenter(exam.getCenterId())) {
            throw new AssessAccessDeniedException();
        }

        return gradeRepository.findByExamId(examId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<GradeResponse> listGradesByStudent(UUID studentId, AuthPrincipal principal) {
        if (!principal.isSuperAdmin() && !principal.userId().equals(studentId)) {
            throw new AssessAccessDeniedException();
        }
        return gradeRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    private GradeResponse toResponse(Grade g) {
        return new GradeResponse(
                g.getId(), g.getSubmissionId(), g.getExamId(), g.getStudentId(),
                g.getBatchId(), g.getCenterId(),
                g.getPercentage(), g.getLetterGrade(), g.isPassed(), g.getCreatedAt()
        );
    }
}
