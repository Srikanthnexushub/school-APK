package com.edutech.performance.application.service;

import com.edutech.performance.application.dto.ReadinessScoreResponse;
import com.edutech.performance.application.exception.ReadinessScoreNotFoundException;
import com.edutech.performance.domain.model.MasteryLevel;
import com.edutech.performance.domain.model.ReadinessScore;
import com.edutech.performance.domain.model.SubjectMastery;
import com.edutech.performance.domain.port.out.PerformanceEventPublisher;
import com.edutech.performance.domain.port.out.PerformanceSnapshotRepository;
import com.edutech.performance.domain.port.out.ReadinessScoreRepository;
import com.edutech.performance.domain.port.out.SubjectMasteryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReadinessScoreService Unit Tests")
class ReadinessScoreServiceTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();

    @Mock
    ReadinessScoreRepository readinessScoreRepository;
    @Mock
    SubjectMasteryRepository subjectMasteryRepository;
    @Mock
    PerformanceSnapshotRepository snapshotRepository;
    @Mock
    PerformanceEventPublisher eventPublisher;

    @InjectMocks
    ReadinessScoreService readinessScoreService;

    @Test
    @DisplayName("computeScore_success: correct ERS formula result (verify weighted sum)")
    void computeScore_success() {
        // Given: 2 subjects — one PROFICIENT (70%), one DEVELOPING (50%)
        SubjectMastery m1 = mock(SubjectMastery.class);
        when(m1.getMasteryPercent()).thenReturn(new BigDecimal("70.00"));
        when(m1.getMasteryLevel()).thenReturn(MasteryLevel.PROFICIENT);

        SubjectMastery m2 = mock(SubjectMastery.class);
        when(m2.getMasteryPercent()).thenReturn(new BigDecimal("50.00"));
        when(m2.getMasteryLevel()).thenReturn(MasteryLevel.DEVELOPING);

        when(subjectMasteryRepository.findByStudentIdAndEnrollmentId(STUDENT_ID, ENROLLMENT_ID))
                .thenReturn(List.of(m1, m2));
        when(readinessScoreRepository.findLatestByStudentIdAndEnrollmentId(STUDENT_ID, ENROLLMENT_ID))
                .thenReturn(Optional.empty());
        when(readinessScoreRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(snapshotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReadinessScoreResponse response = readinessScoreService.computeScore(STUDENT_ID, ENROLLMENT_ID);

        assertThat(response).isNotNull();
        assertThat(response.ersScore()).isNotNull();
        // masteryAvg = (70+50)/2 = 60, syllabusCoverage = 50% (1 out of 2 > BEGINNER)
        // ERS = 0.25*50 + 0.30*0 + 0.25*60 + 0.10*50 + 0.10*50
        //     = 12.5 + 0 + 15 + 5 + 5 = 37.5
        assertThat(response.ersScore()).isEqualByComparingTo(new BigDecimal("37.50"));
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("computeScore_perfectSyllabus: 100% coverage + all mastered -> high ERS")
    void computeScore_perfectSyllabus() {
        SubjectMastery m1 = mock(SubjectMastery.class);
        when(m1.getMasteryPercent()).thenReturn(new BigDecimal("100.00"));
        when(m1.getMasteryLevel()).thenReturn(MasteryLevel.MASTERED);

        SubjectMastery m2 = mock(SubjectMastery.class);
        when(m2.getMasteryPercent()).thenReturn(new BigDecimal("90.00"));
        when(m2.getMasteryLevel()).thenReturn(MasteryLevel.MASTERED);

        when(subjectMasteryRepository.findByStudentIdAndEnrollmentId(STUDENT_ID, ENROLLMENT_ID))
                .thenReturn(List.of(m1, m2));
        when(readinessScoreRepository.findLatestByStudentIdAndEnrollmentId(STUDENT_ID, ENROLLMENT_ID))
                .thenReturn(Optional.empty());
        when(readinessScoreRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(snapshotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReadinessScoreResponse response = readinessScoreService.computeScore(STUDENT_ID, ENROLLMENT_ID);

        assertThat(response.ersScore()).isGreaterThan(new BigDecimal("50"));
    }

    @Test
    @DisplayName("computeScore_noMockData: handles empty mock history gracefully (mockTrend = 0)")
    void computeScore_noMockData() {
        // Empty mastery list — no mock data available
        when(subjectMasteryRepository.findByStudentIdAndEnrollmentId(STUDENT_ID, ENROLLMENT_ID))
                .thenReturn(List.of());
        when(readinessScoreRepository.findLatestByStudentIdAndEnrollmentId(STUDENT_ID, ENROLLMENT_ID))
                .thenReturn(Optional.empty());
        when(readinessScoreRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(snapshotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReadinessScoreResponse response = readinessScoreService.computeScore(STUDENT_ID, ENROLLMENT_ID);

        assertThat(response).isNotNull();
        assertThat(response.mockTestTrendScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getLatestScore_found: returns latest ReadinessScoreResponse")
    void getLatestScore_found() {
        ReadinessScore score = ReadinessScore.compute(
                STUDENT_ID, ENROLLMENT_ID,
                new BigDecimal("80"), new BigDecimal("70"),
                new BigDecimal("75"), new BigDecimal("60"), new BigDecimal("65"));

        when(readinessScoreRepository.findLatestByStudentIdAndEnrollmentId(STUDENT_ID, ENROLLMENT_ID))
                .thenReturn(Optional.of(score));

        ReadinessScoreResponse response = readinessScoreService.getLatestScore(STUDENT_ID, ENROLLMENT_ID);

        assertThat(response).isNotNull();
        assertThat(response.studentId()).isEqualTo(STUDENT_ID);
        assertThat(response.enrollmentId()).isEqualTo(ENROLLMENT_ID);
    }

    @Test
    @DisplayName("getLatestScore_notFound: throws ReadinessScoreNotFoundException")
    void getLatestScore_notFound() {
        when(readinessScoreRepository.findLatestByStudentIdAndEnrollmentId(STUDENT_ID, ENROLLMENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> readinessScoreService.getLatestScore(STUDENT_ID, ENROLLMENT_ID))
                .isInstanceOf(ReadinessScoreNotFoundException.class);
    }
}
