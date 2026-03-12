// src/test/java/com/edutech/assess/application/service/SubmissionServiceTest.java
package com.edutech.assess.application.service;

import com.edutech.assess.application.dto.AnswerEntry;
import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.SubmissionResponse;
import com.edutech.assess.application.dto.SubmitAnswersRequest;
import com.edutech.assess.application.exception.AssessAccessDeniedException;
import com.edutech.assess.application.exception.EnrollmentNotFoundException;
import com.edutech.assess.application.exception.MaxAttemptsExceededException;
import com.edutech.assess.application.exception.SubmissionAlreadySubmittedException;
import com.edutech.assess.domain.model.Exam;
import com.edutech.assess.domain.model.ExamEnrollment;
import com.edutech.assess.domain.model.ExamStatus;
import com.edutech.assess.domain.model.EnrollmentStatus;
import com.edutech.assess.domain.model.Question;
import com.edutech.assess.domain.model.Role;
import com.edutech.assess.domain.model.Submission;
import com.edutech.assess.domain.model.SubmissionStatus;
import com.edutech.assess.domain.port.out.AssessEventPublisher;
import com.edutech.assess.domain.port.out.ExamEnrollmentRepository;
import com.edutech.assess.domain.port.out.ExamRepository;
import com.edutech.assess.domain.port.out.GradeRepository;
import com.edutech.assess.domain.port.out.NotificationEventPort;
import com.edutech.assess.domain.port.out.QuestionRepository;
import com.edutech.assess.domain.port.out.SubmissionAnswerRepository;
import com.edutech.assess.domain.port.out.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SubmissionService Unit Tests")
class SubmissionServiceTest {

    private static final UUID STUDENT_ID    = UUID.randomUUID();
    private static final UUID EXAM_ID       = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();

    @Mock
    SubmissionRepository submissionRepository;

    @Mock
    ExamRepository examRepository;

    @Mock
    QuestionRepository questionRepository;

    @Mock
    ExamEnrollmentRepository enrollmentRepository;

    @Mock
    SubmissionAnswerRepository answerRepository;

    @Mock
    GradeRepository gradeRepository;

    @Mock
    AssessEventPublisher eventPublisher;

    @Mock
    NotificationEventPort notificationEventPort;

    @InjectMocks
    SubmissionService submissionService;

    private AuthPrincipal studentPrincipal() {
        return new AuthPrincipal(STUDENT_ID, "student@test.com", Role.STUDENT, null, "fp");
    }

    private AuthPrincipal otherPrincipal() {
        return new AuthPrincipal(UUID.randomUUID(), "other@test.com", Role.STUDENT, null, "fp");
    }

    private Exam mockPublishedExam() {
        Exam exam = mock(Exam.class);
        when(exam.getStatus()).thenReturn(ExamStatus.PUBLISHED);
        when(exam.getMaxAttempts()).thenReturn(1);
        when(exam.getTotalMarks()).thenReturn(10.0);
        when(exam.getPassingMarks()).thenReturn(6.0);
        when(exam.getBatchId()).thenReturn(UUID.randomUUID());
        when(exam.getCenterId()).thenReturn(UUID.randomUUID());
        return exam;
    }

    private ExamEnrollment mockEnrollment() {
        ExamEnrollment enrollment = mock(ExamEnrollment.class);
        when(enrollment.getId()).thenReturn(ENROLLMENT_ID);
        when(enrollment.getStatus()).thenReturn(EnrollmentStatus.ENROLLED);
        return enrollment;
    }

    @Test
    @DisplayName("startSubmission_success: creates IN_PROGRESS submission with attemptNumber 1")
    void startSubmission_success() {
        Exam exam = mockPublishedExam();
        ExamEnrollment enrollment = mockEnrollment();
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.findByExamIdAndStudentId(EXAM_ID, STUDENT_ID))
                .thenReturn(Optional.of(enrollment));
        when(submissionRepository.countByExamIdAndStudentId(EXAM_ID, STUDENT_ID)).thenReturn(0L);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(i -> i.getArgument(0));

        SubmissionResponse response = submissionService.startSubmission(EXAM_ID, studentPrincipal());

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(SubmissionStatus.IN_PROGRESS);
        assertThat(response.attemptNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("startSubmission_notEnrolled: throws EnrollmentNotFoundException, never saves")
    void startSubmission_notEnrolled() {
        Exam exam = mockPublishedExam();
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.findByExamIdAndStudentId(EXAM_ID, STUDENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.startSubmission(EXAM_ID, studentPrincipal()))
                .isInstanceOf(EnrollmentNotFoundException.class);

        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("startSubmission_maxAttemptsExceeded: throws MaxAttemptsExceededException when limit reached")
    void startSubmission_maxAttemptsExceeded() {
        Exam exam = mockPublishedExam(); // maxAttempts = 1
        ExamEnrollment enrollment = mockEnrollment();
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.findByExamIdAndStudentId(EXAM_ID, STUDENT_ID))
                .thenReturn(Optional.of(enrollment));
        when(submissionRepository.countByExamIdAndStudentId(EXAM_ID, STUDENT_ID)).thenReturn(1L);

        assertThatThrownBy(() -> submissionService.startSubmission(EXAM_ID, studentPrincipal()))
                .isInstanceOf(MaxAttemptsExceededException.class);
    }

    @Test
    @DisplayName("submitAnswers_success: 2 correct answers produce GRADED status with full score and 2 events published")
    void submitAnswers_success() {
        Submission sub = Submission.create(EXAM_ID, STUDENT_ID, ENROLLMENT_ID, 1);

        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();

        Question q1 = mock(Question.class);
        when(q1.getCorrectAnswer()).thenReturn(0);
        when(q1.getMarks()).thenReturn(5.0);

        Question q2 = mock(Question.class);
        when(q2.getCorrectAnswer()).thenReturn(1);
        when(q2.getMarks()).thenReturn(5.0);

        Exam exam = mock(Exam.class);
        when(exam.getTotalMarks()).thenReturn(10.0);
        when(exam.getPassingMarks()).thenReturn(6.0);
        when(exam.getBatchId()).thenReturn(UUID.randomUUID());
        when(exam.getCenterId()).thenReturn(UUID.randomUUID());

        when(submissionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam));
        when(questionRepository.findById(q1Id)).thenReturn(Optional.of(q1));
        when(questionRepository.findById(q2Id)).thenReturn(Optional.of(q2));
        when(answerRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(submissionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(gradeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SubmitAnswersRequest request = new SubmitAnswersRequest(List.of(
                new AnswerEntry(q1Id, 0),  // correct for q1
                new AnswerEntry(q2Id, 1)   // correct for q2
        ));

        SubmissionResponse response = submissionService.submitAnswers(
                EXAM_ID, sub.getId(), request, studentPrincipal());

        assertThat(response.status()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(response.scoredMarks()).isEqualTo(10.0);
        verify(eventPublisher, times(2)).publish(any());
    }

    @Test
    @DisplayName("submitAnswers_alreadySubmitted: throws SubmissionAlreadySubmittedException, never saves answers")
    void submitAnswers_alreadySubmitted() {
        Submission sub = Submission.create(EXAM_ID, STUDENT_ID, ENROLLMENT_ID, 1);
        sub.grade(5.0, 10.0); // already graded = no longer IN_PROGRESS

        when(submissionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> submissionService.submitAnswers(
                EXAM_ID, sub.getId(),
                new SubmitAnswersRequest(List.of(new AnswerEntry(UUID.randomUUID(), 0))),
                studentPrincipal()))
                .isInstanceOf(SubmissionAlreadySubmittedException.class);

        verify(answerRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("submitAnswers_notOwner: throws AssessAccessDeniedException when principal is not submission owner")
    void submitAnswers_notOwner() {
        Submission sub = Submission.create(EXAM_ID, STUDENT_ID, ENROLLMENT_ID, 1);
        // sub.studentId == STUDENT_ID, but otherPrincipal has different userId

        when(submissionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> submissionService.submitAnswers(
                EXAM_ID, sub.getId(),
                new SubmitAnswersRequest(List.of(new AnswerEntry(UUID.randomUUID(), 0))),
                otherPrincipal()))
                .isInstanceOf(AssessAccessDeniedException.class);

        verify(answerRepository, never()).saveAll(any());
    }
}
