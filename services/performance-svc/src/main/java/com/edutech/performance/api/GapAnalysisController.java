package com.edutech.performance.api;

import com.edutech.performance.domain.model.ReadinessScore;
import com.edutech.performance.domain.model.SubjectMastery;
import com.edutech.performance.domain.model.WeakAreaRecord;
import com.edutech.performance.domain.port.out.ReadinessScoreRepository;
import com.edutech.performance.domain.port.out.SubjectMasteryRepository;
import com.edutech.performance.domain.port.out.WeakAreaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Internal gap analysis controller — consumed by nexus-chat-svc and admin dashboards.
 * No JWT required (internal service-to-service call). Read-only — no @Transactional needed.
 */
@RestController
@RequestMapping("/api/v1/performance")
public class GapAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(GapAnalysisController.class);

    // ERS component weights (must match ReadinessScore.compute formula)
    private static final double W_SYLLABUS   = 0.25;
    private static final double W_MOCK       = 0.30;
    private static final double W_MASTERY    = 0.25;
    private static final double W_TIME_MGMT  = 0.10;
    private static final double W_ACCURACY   = 0.10;

    // ERS component targets (reasonable pass-level thresholds)
    private static final double T_SYLLABUS   = 75.0;
    private static final double T_MOCK       = 70.0;
    private static final double T_MASTERY    = 75.0;
    private static final double T_TIME_MGMT  = 70.0;
    private static final double T_ACCURACY   = 70.0;

    // Subject mastery target (general adequacy threshold)
    private static final double SUBJECT_TARGET = 65.0;

    private final ReadinessScoreRepository readinessScoreRepository;
    private final SubjectMasteryRepository subjectMasteryRepository;
    private final WeakAreaRepository weakAreaRepository;

    public GapAnalysisController(ReadinessScoreRepository readinessScoreRepository,
                                  SubjectMasteryRepository subjectMasteryRepository,
                                  WeakAreaRepository weakAreaRepository) {
        this.readinessScoreRepository = readinessScoreRepository;
        this.subjectMasteryRepository = subjectMasteryRepository;
        this.weakAreaRepository = weakAreaRepository;
    }

    /**
     * GET /api/v1/performance/gap-analysis/{studentId}
     *
     * Computes a comprehensive gap analysis from existing performance data:
     * - ERS component breakdown with per-component gaps and bottleneck identification
     * - Subject-level mastery gaps with trend and priority classification
     * - Prerequisite chain gaps (blocking → blocked topic chains)
     * - ERS-based dropout risk summary
     *
     * Returns 200 with zero-filled response when no data exists (new student).
     */
    @GetMapping("/gap-analysis/{studentId}")
    public ResponseEntity<GapAnalysisResponse> getGapAnalysis(@PathVariable UUID studentId) {
        log.debug("Gap analysis requested for studentId={}", studentId);

        Optional<ReadinessScore> latestOpt = readinessScoreRepository.findLatestByStudentId(studentId);

        // ── 1. ERS Breakdown Gap ──────────────────────────────────────────────
        ErsBreakdown ersBreakdown;
        RiskSummary dropoutRisk;

        if (latestOpt.isEmpty()) {
            log.debug("No readiness score found for studentId={} — returning zero-filled response", studentId);
            ersBreakdown = buildEmptyErsBreakdown();
            dropoutRisk = new RiskSummary("UNKNOWN", 0.0, "NO_DATA");
        } else {
            ReadinessScore rs = latestOpt.get();
            double syllabus  = rs.getSyllabusCoveragePercent().doubleValue();
            double mock      = rs.getMockTestTrendScore().doubleValue();
            double mastery   = rs.getMasteryAverage().doubleValue();
            double timeMgmt  = rs.getTimeManagementScore().doubleValue();
            double accuracy  = rs.getAccuracyConsistency().doubleValue();
            double ersTotal  = rs.getErsScore().doubleValue();

            ErsComponent syllabusComp  = new ErsComponent(syllabus,  W_SYLLABUS,  Math.max(0, T_SYLLABUS  - syllabus));
            ErsComponent mockComp      = new ErsComponent(mock,      W_MOCK,      Math.max(0, T_MOCK      - mock));
            ErsComponent masteryComp   = new ErsComponent(mastery,   W_MASTERY,   Math.max(0, T_MASTERY   - mastery));
            ErsComponent timeMgmtComp  = new ErsComponent(timeMgmt,  W_TIME_MGMT, Math.max(0, T_TIME_MGMT - timeMgmt));
            ErsComponent accuracyComp  = new ErsComponent(accuracy,  W_ACCURACY,  Math.max(0, T_ACCURACY  - accuracy));

            ersBreakdown = new ErsBreakdown(ersTotal, syllabusComp, mockComp, masteryComp, timeMgmtComp, accuracyComp);

            // ── 4. ERS Risk ───────────────────────────────────────────────────
            String riskLevel = ersTotal < 40.0 ? "HIGH" : ersTotal < 65.0 ? "MEDIUM" : "LOW";
            String topFactor = identifyBottleneck(syllabusComp, mockComp, masteryComp, timeMgmtComp, accuracyComp);
            dropoutRisk = new RiskSummary(riskLevel, ersTotal, topFactor);
        }

        // ── 2. Subject Gap Analysis ───────────────────────────────────────────
        List<SubjectMastery> masteries = subjectMasteryRepository.findByStudentId(studentId);
        List<SubjectGap> subjectGaps = buildSubjectGaps(studentId, masteries);

        // ── 3. Prerequisite Chain Gap ─────────────────────────────────────────
        List<WeakAreaRecord> allWeakAreas = weakAreaRepository.findByStudentIdOrderByMasteryAsc(studentId, 200);
        List<PrerequisiteChain> prerequisiteChains = buildPrerequisiteChains(allWeakAreas);

        GapAnalysisResponse response = new GapAnalysisResponse(
                studentId,
                Instant.now(),
                ersBreakdown,
                subjectGaps,
                prerequisiteChains,
                dropoutRisk
        );

        return ResponseEntity.ok(response);
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private String identifyBottleneck(ErsComponent syllabus, ErsComponent mock,
                                       ErsComponent mastery, ErsComponent timeMgmt,
                                       ErsComponent accuracy) {
        record ComponentScore(String name, double weightedGap) {}
        List<ComponentScore> scores = List.of(
                new ComponentScore("syllabusCoverage", syllabus.gap() * syllabus.weight()),
                new ComponentScore("mockTestTrend",    mock.gap()     * mock.weight()),
                new ComponentScore("masteryAverage",   mastery.gap()  * mastery.weight()),
                new ComponentScore("timeManagement",   timeMgmt.gap() * timeMgmt.weight()),
                new ComponentScore("accuracy",         accuracy.gap() * accuracy.weight())
        );
        return scores.stream()
                .max(Comparator.comparingDouble(ComponentScore::weightedGap))
                .map(ComponentScore::name)
                .orElse("NONE");
    }

    private List<SubjectGap> buildSubjectGaps(UUID studentId, List<SubjectMastery> masteries) {
        List<SubjectGap> gaps = new ArrayList<>();
        for (SubjectMastery m : masteries) {
            double pct     = m.getMasteryPercent().doubleValue();
            double vel     = m.getVelocityPerWeek() != null ? m.getVelocityPerWeek().doubleValue() : 0.0;
            double gapPts  = Math.max(0.0, SUBJECT_TARGET - pct);
            String priority = derivePriority(pct);
            String trend    = deriveTrend(vel);
            String level    = m.getMasteryLevel() != null ? m.getMasteryLevel().name() : "UNKNOWN";

            // Fetch top 3 weak topics for this subject (already ordered by mastery asc)
            List<WeakAreaRecord> subjectWeak = weakAreaRepository.findByStudentIdAndSubject(studentId, m.getSubject());
            List<String> topWeakTopics = subjectWeak.stream()
                    .sorted(Comparator.comparing(w -> w.getMasteryPercent()))
                    .limit(3)
                    .map(WeakAreaRecord::getTopicName)
                    .collect(Collectors.toList());

            gaps.add(new SubjectGap(
                    m.getSubject(),
                    pct,
                    level,
                    vel,
                    trend,
                    SUBJECT_TARGET,
                    gapPts,
                    priority,
                    topWeakTopics
            ));
        }
        // Sort by gapPoints descending so highest-gap subjects appear first
        gaps.sort(Comparator.comparingDouble(SubjectGap::gapPoints).reversed());
        return gaps;
    }

    private List<PrerequisiteChain> buildPrerequisiteChains(List<WeakAreaRecord> allWeakAreas) {
        // Filter to only weak areas where prerequisites are flagged as weak
        List<WeakAreaRecord> prereqWeak = allWeakAreas.stream()
                .filter(w -> Boolean.TRUE.equals(w.getPrerequisitesWeak()))
                .collect(Collectors.toList());

        // Group by subject
        Map<String, List<WeakAreaRecord>> bySubject = prereqWeak.stream()
                .collect(Collectors.groupingBy(WeakAreaRecord::getSubject));

        List<PrerequisiteChain> chains = new ArrayList<>();
        for (Map.Entry<String, List<WeakAreaRecord>> entry : bySubject.entrySet()) {
            String subject = entry.getKey();
            List<WeakAreaRecord> group = entry.getValue();

            if (group.isEmpty()) continue;

            // Blocking topic = lowest mastery in the group (the root prerequisite gap)
            WeakAreaRecord blocker = group.stream()
                    .min(Comparator.comparing(w -> w.getMasteryPercent()))
                    .orElse(group.get(0));

            List<String> blockedTopics = group.stream()
                    .filter(w -> !w.getId().equals(blocker.getId()))
                    .map(WeakAreaRecord::getTopicName)
                    .collect(Collectors.toList());

            chains.add(new PrerequisiteChain(blocker.getTopicName(), blockedTopics, subject));
        }
        return chains;
    }

    private String derivePriority(double masteryPercent) {
        if (masteryPercent < 30.0) return "CRITICAL";
        if (masteryPercent < 50.0) return "HIGH";
        if (masteryPercent < 65.0) return "MEDIUM";
        return "ON_TRACK";
    }

    private String deriveTrend(double velocityPerWeek) {
        if (velocityPerWeek < -0.5) return "DECLINING";
        if (velocityPerWeek >  0.5) return "IMPROVING";
        return "FLAT";
    }

    private ErsBreakdown buildEmptyErsBreakdown() {
        ErsComponent zero = new ErsComponent(0.0, 0.0, 0.0);
        return new ErsBreakdown(0.0, zero, zero, zero, zero, zero);
    }

    // ─── Response Records ─────────────────────────────────────────────────────

    public record GapAnalysisResponse(
            UUID studentId,
            Instant computedAt,
            ErsBreakdown ersBreakdown,
            List<SubjectGap> subjectGaps,
            List<PrerequisiteChain> prerequisiteChains,
            RiskSummary dropoutRisk
    ) {}

    public record ErsBreakdown(
            double total,
            ErsComponent syllabusCoverage,
            ErsComponent mockTestTrend,
            ErsComponent masteryAverage,
            ErsComponent timeManagement,
            ErsComponent accuracy
    ) {}

    public record ErsComponent(double score, double weight, double gap) {}

    public record SubjectGap(
            String subject,
            double masteryPercent,
            String masteryLevel,
            double velocityPerWeek,
            String trend,
            double targetPercent,
            double gapPoints,
            String priority,
            List<String> topWeakTopics
    ) {}

    public record PrerequisiteChain(
            String blockingTopic,
            List<String> blockedTopics,
            String subject
    ) {}

    public record RiskSummary(String level, double score, String topFactor) {}
}
