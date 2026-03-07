package com.edutech.examtracker.application.service;

import com.edutech.examtracker.application.dto.MockTestAttemptResponse;
import com.edutech.examtracker.application.dto.RecordMockTestRequest;
import com.edutech.examtracker.domain.model.ExamCode;
import com.edutech.examtracker.domain.model.MockTestAttempt;
import com.edutech.examtracker.domain.port.out.ExamTrackerEventPublisher;
import com.edutech.examtracker.domain.port.out.MockTestAttemptRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MockTestService unit tests")
class MockTestServiceTest {

    @Mock
    MockTestAttemptRepository mockTestAttemptRepository;

    @Mock
    ExamTrackerEventPublisher eventPublisher;

    @InjectMocks
    MockTestService mockTestService;

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();

    private RecordMockTestRequest buildRequest(int totalQuestions, int attempted, int correct, int incorrect) {
        return new RecordMockTestRequest(
                ENROLLMENT_ID,
                "JEE Main Full Mock #3",
                ExamCode.JEE_MAIN,
                LocalDate.now(),
                totalQuestions,
                attempted,
                correct,
                incorrect,
                new BigDecimal("240.00"),
                new BigDecimal("300.00"),
                120,
                180,
                null
        );
    }

    @Test
    @DisplayName("recordMockTest_success: calculates accuracy correctly (correct/attempted * 100), saves, publishes event")
    void recordMockTest_success() {
        RecordMockTestRequest request = buildRequest(90, 80, 60, 20);
        when(mockTestAttemptRepository.save(any(MockTestAttempt.class)))
                .thenAnswer(i -> i.getArgument(0));

        MockTestAttemptResponse response = mockTestService.recordMockTest(STUDENT_ID, request);

        assertThat(response).isNotNull();
        assertThat(response.studentId()).isEqualTo(STUDENT_ID);
        assertThat(response.testName()).isEqualTo("JEE Main Full Mock #3");
        // accuracy = 60/80 * 100 = 75.00
        assertThat(response.accuracyPercent()).isEqualByComparingTo(new BigDecimal("75.00"));
        verify(mockTestAttemptRepository).save(any(MockTestAttempt.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("recordMockTest_perfectScore: 100% accuracy case")
    void recordMockTest_perfectScore() {
        RecordMockTestRequest request = buildRequest(90, 90, 90, 0);
        when(mockTestAttemptRepository.save(any(MockTestAttempt.class)))
                .thenAnswer(i -> i.getArgument(0));

        MockTestAttemptResponse response = mockTestService.recordMockTest(STUDENT_ID, request);

        assertThat(response.accuracyPercent()).isEqualByComparingTo(new BigDecimal("100.00"));
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("getMockHistory_returnsOrdered: returns list sorted by attemptDate desc")
    void getMockHistory_returnsOrdered() {
        MockTestAttempt attempt1 = MockTestAttempt.create(
                STUDENT_ID, ENROLLMENT_ID, "Mock #1", ExamCode.JEE_MAIN,
                LocalDate.now().minusDays(7), 90, 80, 60, 20,
                new BigDecimal("240"), new BigDecimal("300"), new BigDecimal("75.00"),
                120, 180, null);
        MockTestAttempt attempt2 = MockTestAttempt.create(
                STUDENT_ID, ENROLLMENT_ID, "Mock #2", ExamCode.JEE_MAIN,
                LocalDate.now(), 90, 85, 70, 15,
                new BigDecimal("260"), new BigDecimal("300"), new BigDecimal("82.35"),
                115, 180, null);

        when(mockTestAttemptRepository.findByStudentIdAndEnrollmentId(STUDENT_ID, ENROLLMENT_ID))
                .thenReturn(List.of(attempt1, attempt2));

        List<MockTestAttemptResponse> history = mockTestService.getMockHistory(STUDENT_ID, ENROLLMENT_ID);

        assertThat(history).hasSize(2);
        // Most recent first
        assertThat(history.get(0).attemptDate()).isAfterOrEqualTo(history.get(1).attemptDate());
    }

    @Test
    @DisplayName("recordMockTest_zeroAttempted: handles edge case (no divide by zero — accuracy = 0)")
    void recordMockTest_zeroAttempted() {
        RecordMockTestRequest request = buildRequest(90, 0, 0, 0);
        when(mockTestAttemptRepository.save(any(MockTestAttempt.class)))
                .thenAnswer(i -> i.getArgument(0));

        MockTestAttemptResponse response = mockTestService.recordMockTest(STUDENT_ID, request);

        assertThat(response.accuracyPercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
