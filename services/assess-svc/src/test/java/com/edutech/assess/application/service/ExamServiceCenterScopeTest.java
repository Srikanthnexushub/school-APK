// src/test/java/com/edutech/assess/application/service/ExamServiceCenterScopeTest.java
package com.edutech.assess.application.service;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.StudentExamResponse;
import com.edutech.assess.domain.model.Exam;
import com.edutech.assess.domain.model.ExamMode;
import com.edutech.assess.domain.model.ExamStatus;
import com.edutech.assess.domain.model.Role;
import com.edutech.assess.domain.port.out.AssessEventPublisher;
import com.edutech.assess.domain.port.out.ExamEnrollmentRepository;
import com.edutech.assess.domain.port.out.ExamRepository;
import com.edutech.assess.domain.port.out.NotificationEventPort;
import com.edutech.assess.domain.port.out.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * REGRESSION unit tests for ExamService — Fix #352.
 *
 * <p>Prior to Fix #352, {@code listPublishedExams(studentId, centerId, pageable)} did not exist.
 * The old method called {@code examRepository.findAllPublished()} which returned exams from
 * ALL centers — a cross-center data leak. Students from Center A could see Center B's exams.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>When centerId is null → returns empty page, never calls findAllPublished()</li>
 *   <li>When centerId is provided → calls findPublishedByCenterId(centerId), not findAllPublished()</li>
 *   <li>Exams from a different center are never included in the result</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("REGRESSION Fix#352 — ExamService center-scoped published exam listing")
class ExamServiceCenterScopeTest {

    @Mock ExamRepository examRepository;
    @Mock QuestionRepository questionRepository;
    @Mock ExamEnrollmentRepository enrollmentRepository;
    @Mock AssessEventPublisher eventPublisher;
    @Mock NotificationEventPort notificationEventPort;

    @InjectMocks
    ExamService examService;

    static final UUID STUDENT_ID  = UUID.randomUUID();
    static final UUID CENTER_A    = UUID.randomUUID();
    static final UUID CENTER_B    = UUID.randomUUID();

    static final String EXAM_A_TITLE = "Regression Exam — Center A";
    static final String EXAM_B_TITLE = "Regression Exam — Center B";

    Exam examFromCenterA;
    Exam examFromCenterB;

    @BeforeEach
    void setUp() {
        examFromCenterA = buildPublishedExam(CENTER_A, EXAM_A_TITLE);
        examFromCenterB = buildPublishedExam(CENTER_B, EXAM_B_TITLE);

        when(examRepository.findPublishedByCenterId(CENTER_A)).thenReturn(List.of(examFromCenterA));
        when(examRepository.findPublishedByCenterId(CENTER_B)).thenReturn(List.of(examFromCenterB));
        when(enrollmentRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of());
        when(questionRepository.countByExamId(any())).thenReturn(5);
    }

    // =========================================================================
    // Test 1: null centerId → empty page, findAllPublished NEVER called
    // =========================================================================

    @Test
    @DisplayName("Fix#352 — null centerId returns empty page (student not yet linked to center)")
    void listPublishedExams_nullCenterId_returnsEmptyPage() {
        Page<StudentExamResponse> result = examService.listPublishedExams(
                STUDENT_ID, null, PageRequest.of(0, 50));

        assertThat(result.getContent())
                .as("Student with null centerId must receive an empty exam list (Fix #352)")
                .isEmpty();
        assertThat(result.getTotalElements()).isZero();

        // Must NEVER call findAllPublished — that leaks all centers' data
        verify(examRepository, never()).findAllPublished();
    }

    // =========================================================================
    // Test 2: centerId provided → calls findPublishedByCenterId, NOT findAllPublished
    // =========================================================================

    @Test
    @DisplayName("Fix#352 — centerId provided → findPublishedByCenterId called, findAllPublished never called")
    void listPublishedExams_withCenterId_callsCenterScopedQuery() {
        examService.listPublishedExams(STUDENT_ID, CENTER_A, PageRequest.of(0, 50));

        verify(examRepository).findPublishedByCenterId(CENTER_A);
        verify(examRepository, never()).findAllPublished();
    }

    // =========================================================================
    // Test 3: Student from Center A must NOT see Center B's exams
    // =========================================================================

    @Test
    @DisplayName("Fix#352 — Student from Center A cannot see Center B exams (no cross-center leak)")
    void listPublishedExams_centerAStudent_doesNotSeeCenterBExam() {
        Page<StudentExamResponse> result = examService.listPublishedExams(
                STUDENT_ID, CENTER_A, PageRequest.of(0, 50));

        assertThat(result.getContent())
                .as("Center A student must not see Center B's exam (Fix #352)")
                .noneMatch(e -> EXAM_B_TITLE.equals(e.name()));
    }

    // =========================================================================
    // Test 4: Student from Center A sees Center A's own exams
    // =========================================================================

    @Test
    @DisplayName("Fix#352 — Student from Center A sees their own center's published exams")
    void listPublishedExams_centerAStudent_seesOwnCenterExams() {
        Page<StudentExamResponse> result = examService.listPublishedExams(
                STUDENT_ID, CENTER_A, PageRequest.of(0, 50));

        assertThat(result.getContent())
                .as("Center A student must see their own published exams (Fix #352)")
                .anyMatch(e -> EXAM_A_TITLE.equals(e.name()));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Exam buildPublishedExam(UUID centerId, String title) {
        Exam exam = Exam.create(
                UUID.randomUUID(),    // batchId
                centerId,
                title,
                null,
                ExamMode.ONLINE,
                60, 1,
                null, null,
                100.0, 40.0
        );
        exam.publish(); // transition to PUBLISHED so it appears in published queries
        return exam;
    }
}
