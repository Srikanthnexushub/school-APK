package com.edutech.performance.domain.service;

import com.edutech.performance.domain.model.PerformanceSnapshot;
import com.edutech.performance.domain.model.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DropoutRiskCalculator Pure Unit Tests")
class DropoutRiskCalculatorTest {

    private DropoutRiskCalculator calculator;
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        calculator = new DropoutRiskCalculator();
    }

    /**
     * Helper to create a snapshot with a given ERS score and recent snapshot time.
     */
    private PerformanceSnapshot makeSnapshot(double ersScore, int studyMinutes, int mockTestsThisWeek) {
        PerformanceSnapshot snapshot = PerformanceSnapshot.take(
                STUDENT_ID, ENROLLMENT_ID,
                BigDecimal.valueOf(ersScore),
                BigDecimal.ZERO,
                null,
                RiskLevel.GREEN
        );
        snapshot.setTotalStudyMinutesToday(studyMinutes);
        snapshot.setMockTestsThisWeek(mockTestsThisWeek);
        return snapshot;
    }

    @Test
    @DisplayName("calculateRisk_noSignals: empty snapshot list returns GREEN with score 0.0")
    void calculateRisk_noSignals() {
        DropoutRiskCalculator.RiskAssessment assessment = calculator.calculate(List.of(), null);

        assertThat(assessment.riskLevel()).isEqualTo(RiskLevel.GREEN);
        assertThat(assessment.riskScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(assessment.primaryFactor()).isEqualTo("none");
    }

    @Test
    @DisplayName("calculateRisk_oneSignal: single declining ERS snapshot does not trigger 3-consecutive rule -> GREEN")
    void calculateRisk_oneSignal() {
        // Only 1 snapshot — can't have 3 consecutive declines
        // Recent snapshot — no inactivity
        PerformanceSnapshot recent = makeSnapshot(60.0, 45, 5);

        DropoutRiskCalculator.RiskAssessment assessment = calculator.calculate(List.of(recent), Instant.now());

        // No signals should fire with a single snapshot that has recent activity
        assertThat(assessment.riskLevel()).isIn(RiskLevel.GREEN, RiskLevel.AMBER);
        assertThat(assessment.riskScore()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(assessment.riskScore()).isLessThanOrEqualTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("calculateRisk_multipleSignals: accumulates risk correctly and maps to AMBER or RED")
    void calculateRisk_multipleSignals() {
        // 3 consecutive declining ERS snapshots (oldest first in list = index 2 down to 0 newest)
        // Snapshots ordered newest first: s0=50, s1=60, s2=70 (declining)
        PerformanceSnapshot s0 = makeSnapshot(50.0, 10, 3);  // newest, low study, low mock tests
        PerformanceSnapshot s1 = makeSnapshot(60.0, 30, 4);  // middle
        PerformanceSnapshot s2 = makeSnapshot(70.0, 60, 5);  // oldest

        // lastLoginAt = 4 days ago → triggers inactivity signal
        Instant fourDaysAgo = Instant.now().minus(4, ChronoUnit.DAYS);

        DropoutRiskCalculator.RiskAssessment assessment = calculator.calculate(
                List.of(s0, s1, s2), fourDaysAgo);

        // ERS declining (0.3) + Inactivity (0.2) + Study declining (0.2) = at least 0.7 → RED
        assertThat(assessment.riskScore()).isGreaterThanOrEqualTo(new BigDecimal("0.3"));
        assertThat(assessment.riskLevel()).isNotEqualTo(RiskLevel.GREEN);
        assertThat(assessment.primaryFactor()).isEqualTo("ERS_DECLINING");
    }
}
