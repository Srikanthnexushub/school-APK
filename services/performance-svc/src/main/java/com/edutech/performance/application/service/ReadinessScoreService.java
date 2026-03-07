package com.edutech.performance.application.service;

import com.edutech.performance.application.dto.ReadinessScoreResponse;
import com.edutech.performance.application.exception.ReadinessScoreNotFoundException;
import com.edutech.performance.domain.event.ReadinessScoreUpdatedEvent;
import com.edutech.performance.domain.model.MasteryLevel;
import com.edutech.performance.domain.model.PerformanceSnapshot;
import com.edutech.performance.domain.model.ReadinessScore;
import com.edutech.performance.domain.model.RiskLevel;
import com.edutech.performance.domain.model.SubjectMastery;
import com.edutech.performance.domain.port.in.ComputeReadinessScoreUseCase;
import com.edutech.performance.domain.port.in.GetReadinessScoreUseCase;
import com.edutech.performance.domain.port.out.PerformanceEventPublisher;
import com.edutech.performance.domain.port.out.PerformanceSnapshotRepository;
import com.edutech.performance.domain.port.out.ReadinessScoreRepository;
import com.edutech.performance.domain.port.out.SubjectMasteryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReadinessScoreService implements ComputeReadinessScoreUseCase, GetReadinessScoreUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReadinessScoreService.class);

    private final ReadinessScoreRepository readinessScoreRepository;
    private final SubjectMasteryRepository subjectMasteryRepository;
    private final PerformanceSnapshotRepository snapshotRepository;
    private final PerformanceEventPublisher eventPublisher;

    public ReadinessScoreService(ReadinessScoreRepository readinessScoreRepository,
                                  SubjectMasteryRepository subjectMasteryRepository,
                                  PerformanceSnapshotRepository snapshotRepository,
                                  PerformanceEventPublisher eventPublisher) {
        this.readinessScoreRepository = readinessScoreRepository;
        this.subjectMasteryRepository = subjectMasteryRepository;
        this.snapshotRepository = snapshotRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ReadinessScoreResponse computeScore(UUID studentId, UUID enrollmentId) {
        log.info("Computing ERS for studentId={} enrollmentId={}", studentId, enrollmentId);

        List<SubjectMastery> masteries = subjectMasteryRepository.findByStudentIdAndEnrollmentId(studentId, enrollmentId);

        BigDecimal masteryAverage = computeMasteryAverage(masteries);
        BigDecimal syllabusCoveragePercent = computeSyllabusCoverage(masteries);
        BigDecimal mockTrendScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal timeManagementScore = new BigDecimal("50.00");
        BigDecimal accuracyConsistency = new BigDecimal("50.00");

        BigDecimal previousErs = readinessScoreRepository
                .findLatestByStudentIdAndEnrollmentId(studentId, enrollmentId)
                .map(ReadinessScore::getErsScore)
                .orElse(BigDecimal.ZERO);

        ReadinessScore score = ReadinessScore.compute(
                studentId, enrollmentId,
                syllabusCoveragePercent,
                mockTrendScore,
                masteryAverage,
                timeManagementScore,
                accuracyConsistency
        );

        ReadinessScore saved = readinessScoreRepository.save(score);
        log.info("Saved ERS={} for studentId={}", saved.getErsScore(), studentId);

        PerformanceSnapshot snapshot = PerformanceSnapshot.take(
                studentId, enrollmentId,
                saved.getErsScore(),
                BigDecimal.ZERO,
                null,
                RiskLevel.GREEN
        );
        snapshotRepository.save(snapshot);

        eventPublisher.publish(new ReadinessScoreUpdatedEvent(
                UUID.randomUUID().toString(),
                studentId,
                enrollmentId,
                saved.getErsScore(),
                previousErs,
                saved.getProjectedRank(),
                Instant.now()
        ));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReadinessScoreResponse getLatestScore(UUID studentId, UUID enrollmentId) {
        return readinessScoreRepository.findLatestByStudentIdAndEnrollmentId(studentId, enrollmentId)
                .map(this::toResponse)
                .orElseThrow(() -> new ReadinessScoreNotFoundException(studentId, enrollmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadinessScoreResponse> getScoreHistory(UUID studentId, UUID enrollmentId) {
        return readinessScoreRepository.findByStudentIdAndEnrollmentId(studentId, enrollmentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private BigDecimal computeMasteryAverage(List<SubjectMastery> masteries) {
        if (masteries.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sum = masteries.stream()
                .map(SubjectMastery::getMasteryPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(masteries.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeSyllabusCoverage(List<SubjectMastery> masteries) {
        if (masteries.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long coveredTopics = masteries.stream()
                .filter(m -> m.getMasteryLevel().ordinal() > MasteryLevel.DEVELOPING.ordinal())
                .count();
        return BigDecimal.valueOf(coveredTopics)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(masteries.size()), 2, RoundingMode.HALF_UP);
    }

    private ReadinessScoreResponse toResponse(ReadinessScore score) {
        return new ReadinessScoreResponse(
                score.getId(),
                score.getStudentId(),
                score.getEnrollmentId(),
                score.getErsScore(),
                score.getSyllabusCoveragePercent(),
                score.getMockTestTrendScore(),
                score.getMasteryAverage(),
                score.getAccuracyConsistency(),
                score.getProjectedRank(),
                score.getProjectedPercentile(),
                score.getComputedAt()
        );
    }
}
