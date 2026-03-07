package com.edutech.performance.domain.service;

import com.edutech.performance.domain.model.PerformanceSnapshot;
import com.edutech.performance.domain.model.RiskLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Pure domain service — no Spring annotations.
 * Rule-based dropout risk scoring (placeholder for LSTM model).
 *
 * Risk signals:
 *   - ERS declining for 3+ consecutive snapshots → +0.3
 *   - No login in 3+ days → +0.2  (approximated by no snapshot in 3+ days)
 *   - Study session count declining → +0.2
 *   - Accuracy consistently < 40% → +0.2  (ERS < 40 used as proxy)
 *   - Missed 3+ study sessions this week → +0.1
 * Total: 0.0–1.0, mapped to RiskLevel
 */
public class DropoutRiskCalculator {

    private static final BigDecimal SIGNAL_ERS_DECLINING = new BigDecimal("0.3");
    private static final BigDecimal SIGNAL_INACTIVE = new BigDecimal("0.2");
    private static final BigDecimal SIGNAL_STUDY_DECLINING = new BigDecimal("0.2");
    private static final BigDecimal SIGNAL_LOW_ACCURACY = new BigDecimal("0.2");
    private static final BigDecimal SIGNAL_MISSED_SESSIONS = new BigDecimal("0.1");
    private static final BigDecimal ERS_LOW_THRESHOLD = new BigDecimal("40.0");
    private static final long INACTIVITY_DAYS = 3L;
    private static final int MISSED_SESSION_THRESHOLD = 3;

    public record RiskAssessment(BigDecimal riskScore, RiskLevel riskLevel, String primaryFactor) {
    }

    /**
     * Calculates dropout risk based on recent performance snapshots.
     *
     * @param snapshots list of recent snapshots, ordered by snapshotAt descending (newest first)
     * @param lastLoginAt the last known login time (may be null if unavailable)
     * @return RiskAssessment with score, level and primary driving factor
     */
    public RiskAssessment calculate(List<PerformanceSnapshot> snapshots, Instant lastLoginAt) {
        BigDecimal riskScore = BigDecimal.ZERO;
        String primaryFactor = "none";

        if (snapshots == null || snapshots.isEmpty()) {
            return new RiskAssessment(riskScore, RiskLevel.GREEN, primaryFactor);
        }

        // Signal 1: ERS declining for 3+ consecutive snapshots
        if (isErsDeclining(snapshots)) {
            riskScore = riskScore.add(SIGNAL_ERS_DECLINING);
            primaryFactor = "ERS_DECLINING";
        }

        // Signal 2: No login / no snapshot in 3+ days
        boolean inactive = isInactive(snapshots, lastLoginAt);
        if (inactive) {
            riskScore = riskScore.add(SIGNAL_INACTIVE);
            if ("none".equals(primaryFactor)) {
                primaryFactor = "INACTIVITY";
            }
        }

        // Signal 3: Study session count declining
        if (isStudySessionCountDeclining(snapshots)) {
            riskScore = riskScore.add(SIGNAL_STUDY_DECLINING);
            if ("none".equals(primaryFactor)) {
                primaryFactor = "STUDY_DECLINING";
            }
        }

        // Signal 4: Accuracy consistently < 40% (approximated by ERS < 40)
        if (isLowAccuracy(snapshots)) {
            riskScore = riskScore.add(SIGNAL_LOW_ACCURACY);
            if ("none".equals(primaryFactor)) {
                primaryFactor = "LOW_ACCURACY";
            }
        }

        // Signal 5: Missed 3+ study sessions this week
        if (hasMissedSessions(snapshots)) {
            riskScore = riskScore.add(SIGNAL_MISSED_SESSIONS);
            if ("none".equals(primaryFactor)) {
                primaryFactor = "MISSED_SESSIONS";
            }
        }

        // Cap at 1.0
        if (riskScore.compareTo(BigDecimal.ONE) > 0) {
            riskScore = BigDecimal.ONE;
        }

        riskScore = riskScore.setScale(3, RoundingMode.HALF_UP);
        RiskLevel level = toRiskLevel(riskScore);

        return new RiskAssessment(riskScore, level, primaryFactor);
    }

    private boolean isErsDeclining(List<PerformanceSnapshot> snapshots) {
        if (snapshots.size() < 3) {
            return false;
        }
        // snapshots[0] = newest, check if newest < previous < before that
        BigDecimal s0 = snapshots.get(0).getErsScore();
        BigDecimal s1 = snapshots.get(1).getErsScore();
        BigDecimal s2 = snapshots.get(2).getErsScore();
        return s0.compareTo(s1) < 0 && s1.compareTo(s2) < 0;
    }

    private boolean isInactive(List<PerformanceSnapshot> snapshots, Instant lastLoginAt) {
        Instant referenceTime = snapshots.get(0).getSnapshotAt();
        if (lastLoginAt != null) {
            referenceTime = lastLoginAt.isAfter(referenceTime) ? lastLoginAt : referenceTime;
        }
        long daysSinceActivity = Duration.between(referenceTime, Instant.now()).toDays();
        return daysSinceActivity >= INACTIVITY_DAYS;
    }

    private boolean isStudySessionCountDeclining(List<PerformanceSnapshot> snapshots) {
        if (snapshots.size() < 2) {
            return false;
        }
        int recentMinutes = snapshots.get(0).getTotalStudyMinutesToday();
        int previousMinutes = snapshots.get(1).getTotalStudyMinutesToday();
        return recentMinutes < previousMinutes && previousMinutes > 0;
    }

    private boolean isLowAccuracy(List<PerformanceSnapshot> snapshots) {
        long lowCount = snapshots.stream()
                .limit(3)
                .filter(s -> s.getErsScore().compareTo(ERS_LOW_THRESHOLD) < 0)
                .count();
        return lowCount >= Math.min(3, snapshots.size());
    }

    private boolean hasMissedSessions(List<PerformanceSnapshot> snapshots) {
        int mockTestsThisWeek = snapshots.get(0).getMockTestsThisWeek();
        return mockTestsThisWeek <= (7 - MISSED_SESSION_THRESHOLD);
    }

    public static RiskLevel toRiskLevel(BigDecimal riskScore) {
        double val = riskScore.doubleValue();
        if (val < 0.4) {
            return RiskLevel.GREEN;
        } else if (val < 0.7) {
            return RiskLevel.AMBER;
        } else {
            return RiskLevel.RED;
        }
    }
}
