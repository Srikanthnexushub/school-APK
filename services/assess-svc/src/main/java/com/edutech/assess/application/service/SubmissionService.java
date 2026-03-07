// src/main/java/com/edutech/assess/application/service/SubmissionService.java
package com.edutech.assess.application.service;

import com.edutech.assess.application.dto.AnswerEntry;
import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.SubmissionResponse;
import com.edutech.assess.application.dto.SubmitAnswersRequest;
import com.edutech.assess.application.exception.AssessAccessDeniedException;
import com.edutech.assess.application.exception.EnrollmentNotFoundException;
import com.edutech.assess.application.exception.ExamNotFoundException;
import com.edutech.assess.application.exception.ExamNotPublishedException;
import com.edutech.assess.application.exception.MaxAttemptsExceededException;
import com.edutech.assess.application.exception.QuestionNotFoundException;
import com.edutech.assess.application.exception.SubmissionAlreadySubmittedException;
import com.edutech.assess.application.exception.SubmissionNotFoundException;
import com.edutech.assess.domain.event.ExamSubmittedEvent;
import com.edutech.assess.domain.event.GradeIssuedEvent;
import com.edutech.assess.domain.model.Exam;
import com.edutech.assess.domain.model.ExamEnrollment;
import com.edutech.assess.domain.model.ExamStatus;
import com.edutech.assess.domain.model.Grade;
import com.edutech.assess.domain.model.Question;
import com.edutech.assess.domain.model.Submission;
import com.edutech.assess.domain.model.SubmissionAnswer;
import com.edutech.assess.domain.model.SubmissionStatus;
import com.edutech.assess.domain.port.in.StartSubmissionUseCase;
import com.edutech.assess.domain.port.in.SubmitAnswersUseCase;
import com.edutech.assess.domain.port.out.AssessEventPublisher;
import com.edutech.assess.domain.port.out.ExamEnrollmentRepository;
import com.edutech.assess.domain.port.out.ExamRepository;
import com.edutech.assess.domain.port.out.GradeRepository;
import com.edutech.assess.domain.port.out.QuestionRepository;
import com.edutech.assess.domain.port.out.SubmissionAnswerRepository;
import com.edutech.assess.domain.port.out.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SubmissionService implements StartSubmissionUseCase, SubmitAnswersUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    private final SubmissionRepository submissionRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ExamEnrollmentRepository enrollmentRepository;
    private final SubmissionAnswerRepository answerRepository;
    private final GradeRepository gradeRepository;
    private final AssessEventPublisher eventPublisher;

    public SubmissionService(SubmissionRepository submissionRepository,
                              ExamRepository examRepository,
                              QuestionRepository questionRepository,
                              ExamEnrollmentRepository enrollmentRepository,
                              SubmissionAnswerRepository answerRepository,
                              GradeRepository gradeRepository,
                              AssessEventPublisher eventPublisher) {
        this.submissionRepository = submissionRepository;
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.answerRepository = answerRepository;
        this.gradeRepository = gradeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SubmissionResponse startSubmission(UUID examId, AuthPrincipal principal) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new ExamNotPublishedException(examId);
        }
        UUID studentId = principal.userId();
        ExamEnrollment enrollment = enrollmentRepository.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(studentId, examId));
        long existingCount = submissionRepository.countByExamIdAndStudentId(examId, studentId);
        if (existingCount >= exam.getMaxAttempts()) {
            throw new MaxAttemptsExceededException(examId, exam.getMaxAttempts());
        }
        Submission sub = Submission.create(examId, studentId, enrollment.getId(), (int) existingCount + 1);
        return toSubmissionResponse(submissionRepository.save(sub));
    }

    @Override
    public SubmissionResponse submitAnswers(UUID examId, UUID submissionId,
                                             SubmitAnswersRequest request, AuthPrincipal principal) {
        Submission sub = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
        if (!sub.getStudentId().equals(principal.userId()) && !principal.isSuperAdmin()) {
            throw new AssessAccessDeniedException();
        }
        if (sub.getStatus() != SubmissionStatus.IN_PROGRESS) {
            throw new SubmissionAlreadySubmittedException(submissionId);
        }
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        List<SubmissionAnswer> submissionAnswers = new ArrayList<>();
        for (AnswerEntry entry : request.answers()) {
            Question q = questionRepository.findById(entry.questionId())
                    .orElseThrow(() -> new QuestionNotFoundException(entry.questionId()));
            boolean isCorrect = entry.selectedOption() == q.getCorrectAnswer();
            double marksAwarded = isCorrect ? q.getMarks() : 0.0;
            submissionAnswers.add(SubmissionAnswer.mark(
                    submissionId, entry.questionId(), entry.selectedOption(), isCorrect, marksAwarded
            ));
        }
        answerRepository.saveAll(submissionAnswers);
        double scoredMarks = submissionAnswers.stream()
                .mapToDouble(SubmissionAnswer::getMarksAwarded)
                .sum();
        sub.grade(scoredMarks, exam.getTotalMarks());
        Submission saved = submissionRepository.save(sub);
        double passingPct = exam.getTotalMarks() > 0
                ? (exam.getPassingMarks() / exam.getTotalMarks()) * 100.0
                : 0.0;
        Grade grade = Grade.create(
                saved.getId(), saved.getStudentId(), examId,
                exam.getBatchId(), exam.getCenterId(),
                saved.getPercentage(), passingPct
        );
        Grade savedGrade = gradeRepository.save(grade);
        eventPublisher.publish(new ExamSubmittedEvent(
                saved.getId(), examId, saved.getStudentId(),
                saved.getScoredMarks(), saved.getTotalMarks()
        ));
        eventPublisher.publish(new GradeIssuedEvent(
                savedGrade.getId(), saved.getId(), examId, saved.getStudentId(),
                exam.getBatchId(), exam.getCenterId(),
                saved.getPercentage(), savedGrade.isPassed()
        ));
        log.info("Submission graded: id={} student={} score={}/{}",
                saved.getId(), saved.getStudentId(), saved.getScoredMarks(), saved.getTotalMarks());
        return toSubmissionResponse(saved);
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmission(UUID submissionId, AuthPrincipal principal) {
        Submission sub = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
        if (!principal.isSuperAdmin() && !principal.userId().equals(sub.getStudentId())) {
            throw new AssessAccessDeniedException();
        }
        return toSubmissionResponse(sub);
    }

    private SubmissionResponse toSubmissionResponse(Submission s) {
        return new SubmissionResponse(
                s.getId(), s.getExamId(), s.getStudentId(), s.getStartedAt(),
                s.getSubmittedAt(), s.getTotalMarks(), s.getScoredMarks(), s.getPercentage(),
                s.getStatus(), s.getAttemptNumber()
        );
    }
}
