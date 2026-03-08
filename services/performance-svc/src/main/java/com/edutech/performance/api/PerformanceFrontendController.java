package com.edutech.performance.api;

import com.edutech.performance.domain.model.ReadinessScore;
import com.edutech.performance.domain.model.SubjectMastery;
import com.edutech.performance.domain.model.WeakAreaRecord;
import com.edutech.performance.domain.port.out.AiDropoutRiskPort;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Frontend-facing controller exposing simplified flat URLs for the React dashboard.
 * These endpoints aggregate across all enrollments for a given studentId and return
 * response shapes matched to the frontend's TypeScript interfaces.
 */
@RestController
@RequestMapping("/api/v1/performance")
public class PerformanceFrontendController {

    private static final Logger log = LoggerFactory.getLogger(PerformanceFrontendController.class);

    private final ReadinessScoreRepository readinessScoreRepository;
    private final SubjectMasteryRepository subjectMasteryRepository;
    private final WeakAreaRepository weakAreaRepository;
    private final AiDropoutRiskPort aiDropoutRiskPort;

    public PerformanceFrontendController(ReadinessScoreRepository readinessScoreRepository,
                                          SubjectMasteryRepository subjectMasteryRepository,
                                          WeakAreaRepository weakAreaRepository,
                                          AiDropoutRiskPort aiDropoutRiskPort) {
        this.readinessScoreRepository = readinessScoreRepository;
        this.subjectMasteryRepository = subjectMasteryRepository;
        this.weakAreaRepository = weakAreaRepository;
        this.aiDropoutRiskPort = aiDropoutRiskPort;
    }

    /**
     * GET /api/v1/performance/readiness/{studentId}
     * Returns the latest ERS (Exam Readiness Score) for the student across all enrollments.
     * Returns 200 with null body when no data exists (new student).
     */
    @GetMapping("/readiness/{studentId}")
    public ResponseEntity<ReadinessSummary> getLatestReadiness(@PathVariable UUID studentId) {
        log.debug("Frontend: fetching latest readiness for studentId={}", studentId);
        Optional<ReadinessScore> latest = readinessScoreRepository.findLatestByStudentId(studentId);
        if (latest.isEmpty()) {
            return ResponseEntity.ok(null);
        }
        ReadinessScore score = latest.get();
        ReadinessSummary summary = new ReadinessSummary(
                score.getStudentId(),
                score.getErsScore().doubleValue(),
                score.getComputedAt()
        );
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/v1/performance/mastery/{studentId}
     * Returns all subject mastery records for the student across all enrollments.
     * Returns empty list when no data exists.
     */
    @GetMapping("/mastery/{studentId}")
    public ResponseEntity<List<MasterySummary>> getSubjectMastery(@PathVariable UUID studentId) {
        log.debug("Frontend: fetching subject mastery for studentId={}", studentId);
        List<SubjectMastery> masteries = subjectMasteryRepository.findByStudentId(studentId);
        List<MasterySummary> result = masteries.stream()
                .map(m -> new MasterySummary(
                        m.getSubject(),
                        m.getMasteryPercent().doubleValue(),
                        deriveTrend(m.getVelocityPerWeek()),
                        m.getLastUpdatedAt() != null ? m.getLastUpdatedAt().toString() : m.getUpdatedAt().toString()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/performance/weak-areas/{studentId}
     * Returns all active weak area records for the student across all enrollments.
     * Returns empty list when no data exists.
     */
    @GetMapping("/weak-areas/{studentId}")
    public ResponseEntity<List<WeakAreaSummary>> getWeakAreas(@PathVariable UUID studentId) {
        log.debug("Frontend: fetching weak areas for studentId={}", studentId);
        List<WeakAreaRecord> weakAreas = weakAreaRepository.findByStudentIdOrderByMasteryAsc(studentId, 50);
        List<WeakAreaSummary> result = weakAreas.stream()
                .map(w -> new WeakAreaSummary(
                        w.getId(),
                        w.getSubject(),
                        w.getTopicName(),
                        w.getMasteryPercent().doubleValue(),
                        deriveSeverity(w.getMasteryPercent())
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/performance/dropout-risk/{studentId}
     * Returns a dropout risk assessment for the student.
     * Falls back to a rule-based LOW-risk response when no data exists or AI sidecar is unavailable.
     */
    @GetMapping("/dropout-risk/{studentId}")
    public ResponseEntity<DropoutRiskSummary> getDropoutRisk(@PathVariable UUID studentId) {
        log.debug("Frontend: fetching dropout risk for studentId={}", studentId);

        Optional<ReadinessScore> latestErs = readinessScoreRepository.findLatestByStudentId(studentId);
        BigDecimal ersScore = latestErs.map(ReadinessScore::getErsScore).orElse(BigDecimal.valueOf(50));

        AiDropoutRiskPort.AiRiskPrediction prediction = null;
        try {
            prediction = aiDropoutRiskPort.predictRisk(
                    studentId.toString(),
                    ersScore,
                    BigDecimal.valueOf(80),
                    BigDecimal.valueOf(10),
                    BigDecimal.valueOf(70),
                    0
            );
        } catch (Exception ex) {
            log.warn("AI dropout risk prediction failed for studentId={}: {}", studentId, ex.getMessage());
        }

        DropoutRiskSummary summary;
        if (prediction != null) {
            List<Map<String, Object>> causalFactors = prediction.factors() == null
                    ? List.of()
                    : prediction.factors().stream()
                            .map(f -> Map.<String, Object>of(
                                    "factor", f,
                                    "impact", 25,
                                    "icon", "⚠️"
                            ))
                            .collect(Collectors.toList());
            summary = new DropoutRiskSummary(
                    studentId,
                    prediction.riskScore().doubleValue(),
                    prediction.riskLevel(),
                    causalFactors,
                    prediction.recommendation()
            );
        } else {
            summary = buildFallbackRiskSummary(studentId, ersScore);
        }

        return ResponseEntity.ok(summary);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private String deriveTrend(BigDecimal velocityPerWeek) {
        if (velocityPerWeek == null) return "STABLE";
        int cmp = velocityPerWeek.compareTo(BigDecimal.ZERO);
        if (cmp > 0) return "UP";
        if (cmp < 0) return "DOWN";
        return "STABLE";
    }

    private String deriveSeverity(BigDecimal masteryPercent) {
        double pct = masteryPercent.doubleValue();
        if (pct < 25) return "CRITICAL";
        if (pct < 40) return "HIGH";
        if (pct < 60) return "MEDIUM";
        return "LOW";
    }

    private DropoutRiskSummary buildFallbackRiskSummary(UUID studentId, BigDecimal ersScore) {
        double score = ersScore.doubleValue();
        String riskLevel;
        List<Map<String, Object>> factors;
        String recommendation;

        if (score >= 70) {
            riskLevel = "LOW";
            factors = List.of(
                    Map.of("factor", "Strong ERS score", "impact", 10, "icon", "✅")
            );
            recommendation = "Keep up the great work! Your consistent performance indicates a low risk of dropout.";
        } else if (score >= 50) {
            riskLevel = "MEDIUM";
            factors = List.of(
                    Map.of("factor", "Moderate ERS score", "impact", 40, "icon", "⚠️"),
                    Map.of("factor", "Limited practice data", "impact", 30, "icon", "📊")
            );
            recommendation = "Focus on weak areas and increase mock test frequency to improve readiness.";
        } else {
            riskLevel = "HIGH";
            factors = List.of(
                    Map.of("factor", "Low ERS score", "impact", 60, "icon", "🔴"),
                    Map.of("factor", "Limited practice data", "impact", 40, "icon", "📊")
            );
            recommendation = "Seek help from your mentor and create a structured study plan immediately.";
        }

        double riskScore = Math.max(0, Math.min(100, 100 - score));
        return new DropoutRiskSummary(studentId, riskScore, riskLevel, factors, recommendation);
    }

    // ─── Response Records ──────────────────────────────────────────────────────

    public record ReadinessSummary(UUID studentId, double score, Instant computedAt) {}

    public record MasterySummary(String subject, double score, String trend, String lastUpdated) {}

    public record WeakAreaSummary(UUID id, String subject, String topic, double masteryScore, String severity) {}

    public record DropoutRiskSummary(UUID studentId, double riskScore, String riskLevel,
                                      List<Map<String, Object>> causalFactors, String recommendation) {}
}
