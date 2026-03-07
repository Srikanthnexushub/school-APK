package com.edutech.performance.application.service;

import com.edutech.performance.application.dto.PerformanceDashboardResponse;
import com.edutech.performance.application.dto.PerformanceSnapshotResponse;
import com.edutech.performance.application.dto.ReadinessScoreResponse;
import com.edutech.performance.application.dto.SubjectMasteryResponse;
import com.edutech.performance.application.dto.WeakAreaResponse;
import com.edutech.performance.domain.model.PerformanceSnapshot;
import com.edutech.performance.domain.model.RiskLevel;
import com.edutech.performance.domain.port.in.GetPerformanceDashboardUseCase;
import com.edutech.performance.domain.port.in.GetReadinessScoreUseCase;
import com.edutech.performance.domain.port.in.GetSubjectMasteryUseCase;
import com.edutech.performance.domain.port.in.GetWeakAreasUseCase;
import com.edutech.performance.domain.port.out.PerformanceSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PerformanceDashboardService implements GetPerformanceDashboardUseCase {

    private static final Logger log = LoggerFactory.getLogger(PerformanceDashboardService.class);
    private static final int TOP_WEAK_AREAS_LIMIT = 5;

    private final GetReadinessScoreUseCase readinessScoreUseCase;
    private final GetWeakAreasUseCase weakAreasUseCase;
    private final GetSubjectMasteryUseCase subjectMasteryUseCase;
    private final PerformanceSnapshotRepository snapshotRepository;

    public PerformanceDashboardService(GetReadinessScoreUseCase readinessScoreUseCase,
                                        GetWeakAreasUseCase weakAreasUseCase,
                                        GetSubjectMasteryUseCase subjectMasteryUseCase,
                                        PerformanceSnapshotRepository snapshotRepository) {
        this.readinessScoreUseCase = readinessScoreUseCase;
        this.weakAreasUseCase = weakAreasUseCase;
        this.subjectMasteryUseCase = subjectMasteryUseCase;
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PerformanceDashboardResponse getDashboard(UUID studentId, UUID enrollmentId) {
        log.info("Building performance dashboard for studentId={} enrollmentId={}", studentId, enrollmentId);

        ReadinessScoreResponse latestErs = tryGetLatestErs(studentId, enrollmentId);
        List<WeakAreaResponse> topWeakAreas = weakAreasUseCase.getTopWeakAreas(studentId, enrollmentId, TOP_WEAK_AREAS_LIMIT);
        List<SubjectMasteryResponse> subjectMastery = subjectMasteryUseCase.getSubjectMastery(studentId, enrollmentId);
        Optional<PerformanceSnapshot> latestSnapshotOpt = snapshotRepository.findLatestByStudentId(studentId);

        PerformanceSnapshotResponse latestSnapshot = latestSnapshotOpt.map(this::toSnapshotResponse).orElse(null);

        RiskLevel currentRiskLevel = latestSnapshotOpt.map(PerformanceSnapshot::getRiskLevel).orElse(RiskLevel.GREEN);
        BigDecimal dropoutRiskScore = latestSnapshotOpt.map(PerformanceSnapshot::getDropoutRiskScore)
                .orElse(BigDecimal.ZERO);

        return new PerformanceDashboardResponse(
                studentId,
                enrollmentId,
                latestErs,
                topWeakAreas,
                subjectMastery,
                currentRiskLevel,
                dropoutRiskScore,
                latestSnapshot
        );
    }

    private ReadinessScoreResponse tryGetLatestErs(UUID studentId, UUID enrollmentId) {
        try {
            return readinessScoreUseCase.getLatestScore(studentId, enrollmentId);
        } catch (Exception ex) {
            log.warn("No readiness score found for studentId={} enrollmentId={}", studentId, enrollmentId);
            return null;
        }
    }

    private PerformanceSnapshotResponse toSnapshotResponse(PerformanceSnapshot snapshot) {
        return new PerformanceSnapshotResponse(
                snapshot.getId(),
                snapshot.getErsScore(),
                snapshot.getTheta(),
                snapshot.getPercentile(),
                snapshot.getRiskLevel(),
                snapshot.getDropoutRiskScore(),
                snapshot.getSnapshotAt()
        );
    }
}
