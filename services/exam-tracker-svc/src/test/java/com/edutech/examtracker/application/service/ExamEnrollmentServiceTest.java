package com.edutech.examtracker.application.service;

import com.edutech.examtracker.application.dto.EnrollInExamRequest;
import com.edutech.examtracker.application.dto.ExamEnrollmentResponse;
import com.edutech.examtracker.application.exception.DuplicateEnrollmentException;
import com.edutech.examtracker.application.exception.EnrollmentNotFoundException;
import com.edutech.examtracker.domain.model.ExamCode;
import com.edutech.examtracker.domain.model.ExamEnrollment;
import com.edutech.examtracker.domain.model.ExamStatus;
import com.edutech.examtracker.domain.port.out.ExamEnrollmentRepository;
import com.edutech.examtracker.domain.port.out.ExamTrackerEventPublisher;
import com.edutech.examtracker.domain.port.out.SyllabusModuleRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExamEnrollmentService unit tests")
class ExamEnrollmentServiceTest {

    @Mock
    ExamEnrollmentRepository enrollmentRepository;

    @Mock
    SyllabusModuleRepository syllabusModuleRepository;

    @Mock
    ExamTrackerEventPublisher eventPublisher;

    @InjectMocks
    ExamEnrollmentService examEnrollmentService;

    private static final UUID STUDENT_ID = UUID.randomUUID();

    private EnrollInExamRequest validRequest() {
        return new EnrollInExamRequest(ExamCode.JEE_MAIN, "JEE Main 2026", 2026, null);
    }

    @Test
    @DisplayName("enroll_success: creates enrollment, publishes ExamEnrolledEvent, returns response")
    void enroll_success() {
        when(enrollmentRepository.findByStudentIdAndExamCode(STUDENT_ID, ExamCode.JEE_MAIN))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(ExamEnrollment.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(syllabusModuleRepository.findByEnrollmentId(any())).thenReturn(List.of());

        ExamEnrollmentResponse response = examEnrollmentService.enroll(STUDENT_ID, validRequest());

        assertThat(response).isNotNull();
        assertThat(response.studentId()).isEqualTo(STUDENT_ID);
        assertThat(response.examCode()).isEqualTo(ExamCode.JEE_MAIN);
        assertThat(response.examName()).isEqualTo("JEE Main 2026");
        assertThat(response.targetYear()).isEqualTo(2026);
        assertThat(response.status()).isEqualTo(ExamStatus.ACTIVE);
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("enroll_duplicateExam: throws DuplicateEnrollmentException when same student+examCode exists")
    void enroll_duplicateExam() {
        ExamEnrollment existing = ExamEnrollment.create(STUDENT_ID, ExamCode.JEE_MAIN, "JEE Main 2026", 2026);
        when(enrollmentRepository.findByStudentIdAndExamCode(STUDENT_ID, ExamCode.JEE_MAIN))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> examEnrollmentService.enroll(STUDENT_ID, validRequest()))
                .isInstanceOf(DuplicateEnrollmentException.class);
    }

    @Test
    @DisplayName("getEnrollment_found: returns populated ExamEnrollmentResponse")
    void getEnrollment_found() {
        UUID enrollmentId = UUID.randomUUID();
        ExamEnrollment enrollment = ExamEnrollment.create(STUDENT_ID, ExamCode.NEET_UG, "NEET UG 2026", 2026);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(syllabusModuleRepository.findByEnrollmentId(enrollment.getId())).thenReturn(List.of());

        ExamEnrollmentResponse response = examEnrollmentService.getEnrollment(enrollmentId);

        assertThat(response).isNotNull();
        assertThat(response.examCode()).isEqualTo(ExamCode.NEET_UG);
        assertThat(response.syllabusModulesTotal()).isZero();
        assertThat(response.syllabusModulesCompleted()).isZero();
    }

    @Test
    @DisplayName("getEnrollment_notFound: throws EnrollmentNotFoundException")
    void getEnrollment_notFound() {
        UUID enrollmentId = UUID.randomUUID();
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examEnrollmentService.getEnrollment(enrollmentId))
                .isInstanceOf(EnrollmentNotFoundException.class);
    }

    @Test
    @DisplayName("getStudentEnrollments_returnsAll: returns list sorted by createdAt")
    void getStudentEnrollments_returnsAll() {
        ExamEnrollment e1 = ExamEnrollment.create(STUDENT_ID, ExamCode.JEE_MAIN, "JEE Main 2026", 2026);
        ExamEnrollment e2 = ExamEnrollment.create(STUDENT_ID, ExamCode.GATE, "GATE 2026", 2026);
        when(enrollmentRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(e1, e2));
        when(syllabusModuleRepository.findByEnrollmentId(any())).thenReturn(List.of());

        List<ExamEnrollmentResponse> responses = examEnrollmentService.getStudentEnrollments(STUDENT_ID);

        assertThat(responses).hasSize(2);
    }
}
