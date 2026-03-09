package com.edutech.examtracker.application.service;

import com.edutech.examtracker.application.dto.MockTestAttemptResponse;
import com.edutech.examtracker.application.dto.RecordMockTestRequest;
import com.edutech.examtracker.domain.event.MockTestCompletedEvent;
import com.edutech.examtracker.domain.model.MockTestAttempt;
import com.edutech.examtracker.domain.port.in.GetMockTestHistoryUseCase;
import com.edutech.examtracker.domain.port.in.RecordMockTestUseCase;
import com.edutech.examtracker.domain.port.out.ExamTrackerEventPublisher;
import com.edutech.examtracker.domain.port.out.MockTestAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class MockTestService implements RecordMockTestUseCase, GetMockTestHistoryUseCase {

    private static final Logger log = LoggerFactory.getLogger(MockTestService.class);

    private final MockTestAttemptRepository mockTestAttemptRepository;
    private final ExamTrackerEventPublisher eventPublisher;

    public MockTestService(MockTestAttemptRepository mockTestAttemptRepository,
                           ExamTrackerEventPublisher eventPublisher) {
        this.mockTestAttemptRepository = mockTestAttemptRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public MockTestAttemptResponse recordMockTest(UUID studentId, RecordMockTestRequest request) {
        BigDecimal accuracyPercent = calculateAccuracy(request.correct(), request.attempted());

        MockTestAttempt attempt = MockTestAttempt.create(
                studentId,
                request.enrollmentId(),
                request.testName(),
                request.examCode(),
                request.attemptDate(),
                request.totalQuestions(),
                request.attempted(),
                request.correct(),
                request.incorrect(),
                request.score(),
                request.maxScore(),
                accuracyPercent,
                request.timeTakenMinutes(),
                request.totalTimeMinutes(),
                request.subjectWiseJson()
        );

        MockTestAttempt saved = mockTestAttemptRepository.save(attempt);

        eventPublisher.publish(new MockTestCompletedEvent(
                UUID.randomUUID().toString(),
                studentId,
                request.enrollmentId(),
                request.examCode(),
                saved.getScore(),
                saved.getAccuracyPercent(),
                saved.getEstimatedRank(),
                saved.getAttemptDate(),
                Instant.now()
        ));

        log.info("Mock test recorded: studentId={} testName={} attemptId={}",
                studentId, request.testName(), saved.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MockTestAttemptResponse> getMockHistory(UUID studentId, UUID enrollmentId) {
        return mockTestAttemptRepository.findByStudentIdAndEnrollmentId(studentId, enrollmentId)
                .stream()
                .sorted(Comparator.comparing(MockTestAttempt::getAttemptDate).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MockTestAttemptResponse> getMockHistoryByStudent(UUID studentId) {
        return mockTestAttemptRepository.findByStudentId(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private BigDecimal calculateAccuracy(int correct, int attempted) {
        if (attempted == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(correct)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(attempted), 2, RoundingMode.HALF_UP);
    }

    private MockTestAttemptResponse toResponse(MockTestAttempt a) {
        return new MockTestAttemptResponse(
                a.getId(),
                a.getStudentId(),
                a.getTestName(),
                a.getExamCode(),
                a.getAttemptDate(),
                a.getCorrect(),
                a.getIncorrect(),
                a.getScore(),
                a.getMaxScore(),
                a.getAccuracyPercent(),
                a.getTimeTakenMinutes(),
                a.getEstimatedRank(),
                a.getCreatedAt()
        );
    }
}
