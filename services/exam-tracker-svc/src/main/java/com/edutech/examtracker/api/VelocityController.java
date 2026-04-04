package com.edutech.examtracker.api;

import com.edutech.examtracker.domain.model.ExamEnrollment;
import com.edutech.examtracker.domain.model.ExamStatus;
import com.edutech.examtracker.domain.model.MockTestAttempt;
import com.edutech.examtracker.domain.model.ModuleStatus;
import com.edutech.examtracker.domain.model.StudySession;
import com.edutech.examtracker.domain.model.SyllabusModule;
import com.edutech.examtracker.domain.port.out.ExamEnrollmentRepository;
import com.edutech.examtracker.domain.port.out.MockTestAttemptRepository;
import com.edutech.examtracker.domain.port.out.StudySessionRepository;
import com.edutech.examtracker.domain.port.out.SyllabusModuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exam-tracker")
public class VelocityController {

    private static final Logger log = LoggerFactory.getLogger(VelocityController.class);

    private final ExamEnrollmentRepository enrollmentRepository;
    private final SyllabusModuleRepository syllabusModuleRepository;
    private final MockTestAttemptRepository mockTestAttemptRepository;
    private final StudySessionRepository studySessionRepository;

    public VelocityController(ExamEnrollmentRepository enrollmentRepository,
                              SyllabusModuleRepository syllabusModuleRepository,
                              MockTestAttemptRepository mockTestAttemptRepository,
                              StudySessionRepository studySessionRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.syllabusModuleRepository = syllabusModuleRepository;
        this.mockTestAttemptRepository = mockTestAttemptRepository;
        this.studySessionRepository = studySessionRepository;
    }

    // ─── Response records ──────────────────────────────────────────────────────

    record VelocityResponse(List<EnrollmentVelocity> enrollments) {}

    record EnrollmentVelocity(
            UUID enrollmentId,
            String examName,
            String examCode,
            LocalDate targetDate,
            int daysRemaining,
            SyllabusVelocity syllabus,
            MockTestStats mockTests,
            double studyHoursThisWeek
    ) {}

    record SyllabusVelocity(
            int totalModules,
            int completedModules,
            double completionPercent,
            double requiredDailyModules,
            double actualDailyVelocity,
            boolean onTrack,
            int behindByDays
    ) {}

    record MockTestStats(
            int count,
            double latestScore,
            double maxScore,
            double latestScorePercent,
            String trend,
            double trendSlope,
            double avgAccuracy
    ) {}

    // ─── Endpoint ──────────────────────────────────────────────────────────────

    @GetMapping("/students/{studentId}/velocity")
    public ResponseEntity<VelocityResponse> getVelocity(@PathVariable UUID studentId) {
        log.debug("Computing velocity for student={}", studentId);

        List<ExamEnrollment> enrollments = enrollmentRepository.findByStudentId(studentId);

        List<EnrollmentVelocity> result = new ArrayList<>();

        for (ExamEnrollment enrollment : enrollments) {
            // Only active (not deleted, not dropped/completed) enrollments
            if (enrollment.getDeletedAt() != null) {
                continue;
            }
            if (enrollment.getStatus() != ExamStatus.ACTIVE) {
                continue;
            }

            UUID enrollmentId = enrollment.getId();

            // ── Syllabus velocity ──────────────────────────────────────────
            List<SyllabusModule> modules = syllabusModuleRepository.findByEnrollmentId(enrollmentId);
            int totalModules = modules.size();
            long completedModules = modules.stream()
                    .filter(m -> m.getCompletionPercent() >= 100 || m.getStatus() == ModuleStatus.COMPLETED)
                    .count();

            double completionPercent = totalModules > 0
                    ? (completedModules * 100.0 / totalModules)
                    : 0.0;

            // Days remaining
            LocalDate today = LocalDate.now();
            long daysRemaining;
            if (enrollment.getExamDate() != null) {
                daysRemaining = ChronoUnit.DAYS.between(today, enrollment.getExamDate());
            } else {
                int targetYear = enrollment.getTargetYear();
                daysRemaining = (long)(targetYear - today.getYear()) * 365 - today.getDayOfYear();
            }

            // daysElapsed = max(1, totalDays - daysRemaining), totalDays = daysElapsed + daysRemaining
            // We compute daysElapsed directly: since totalDays = daysElapsed + daysRemaining,
            // and we don't have a fixed start date, derive from a reasonable assumption:
            // totalDays is the full span; use daysRemaining as provided and assume
            // daysElapsed = max(1, 365 - daysRemaining) as a proxy... but the spec says:
            // daysElapsed + daysRemaining = totalDays, and daysElapsed = max(1, totalDays - daysRemaining)
            // The spec gives totalDays = daysElapsed + daysRemaining, so we need another anchor.
            // Use: if examDate set → totalDays = ChronoUnit.DAYS(createdAt.toLocalDate, examDate)
            // The actual formula from the spec: daysElapsed = max(1, totalDays - daysRemaining)
            // where totalDays = daysElapsed + daysRemaining. This is circular unless we treat
            // totalDays as known from enrollment creation to exam date.
            // Use enrollment.getCreatedAt() as the start.
            long daysElapsed;
            if (enrollment.getExamDate() != null) {
                LocalDate startDate = enrollment.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate();
                long totalDays = ChronoUnit.DAYS.between(startDate, enrollment.getExamDate());
                daysElapsed = Math.max(1, totalDays - daysRemaining);
            } else {
                // No exam date: use day-of-year already elapsed as a rough proxy
                // totalDays not definitively known; use today's day-of-year since enrollment creation
                LocalDate startDate = enrollment.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate();
                daysElapsed = Math.max(1, ChronoUnit.DAYS.between(startDate, today));
            }

            double actualDailyVelocity = totalModules > 0
                    ? completedModules / (double) daysElapsed
                    : 0.0;

            double requiredDailyModules = daysRemaining > 0
                    ? (totalModules - completedModules) / (double) daysRemaining
                    : 0.0;

            boolean onTrack = actualDailyVelocity >= requiredDailyModules * 0.9;

            int behindByDays = 0;
            if (!onTrack) {
                double remainingModules = totalModules - completedModules;
                double projectedByVelocity = actualDailyVelocity * daysRemaining;
                double gap = remainingModules - projectedByVelocity;
                behindByDays = (int)(gap / Math.max(0.001, actualDailyVelocity));
            }

            SyllabusVelocity syllabusVelocity = new SyllabusVelocity(
                    totalModules,
                    (int) completedModules,
                    completionPercent,
                    requiredDailyModules,
                    actualDailyVelocity,
                    onTrack,
                    behindByDays
            );

            // ── Mock test trend ────────────────────────────────────────────
            List<MockTestAttempt> mockTests = mockTestAttemptRepository
                    .findByStudentIdAndEnrollmentId(studentId, enrollmentId);
            mockTests = mockTests.stream()
                    .filter(m -> m.getDeletedAt() == null)
                    .sorted(Comparator.comparing(MockTestAttempt::getAttemptDate))
                    .toList();

            int mockCount = mockTests.size();
            double latestScore = 0.0;
            double latestMaxScore = 100.0;
            double latestScorePercent = 0.0;

            if (mockCount > 0) {
                MockTestAttempt last = mockTests.get(mockCount - 1);
                latestScore = last.getScore().doubleValue();
                latestMaxScore = last.getMaxScore().doubleValue();
                latestScorePercent = latestMaxScore > 0 ? latestScore * 100.0 / latestMaxScore : 0.0;
            }

            // Build score percent list for trend
            List<Double> scorePercents = mockTests.stream()
                    .map(m -> {
                        double ms = m.getMaxScore().doubleValue();
                        return ms > 0 ? m.getScore().doubleValue() * 100.0 / ms : 0.0;
                    })
                    .toList();

            double trendSlope = 0.0;
            if (mockCount >= 3) {
                trendSlope = linearRegressionSlope(scorePercents);
            }

            String trend = trendSlope > 2 ? "IMPROVING" : trendSlope < -2 ? "DECLINING" : "FLAT";

            double avgAccuracy = 0.0;
            if (mockCount > 0) {
                avgAccuracy = mockTests.stream()
                        .mapToDouble(m -> m.getTotalQuestions() > 0
                                ? m.getCorrect() * 100.0 / m.getTotalQuestions()
                                : 0.0)
                        .average()
                        .orElse(0.0);
            }

            MockTestStats mockTestStats = new MockTestStats(
                    mockCount,
                    latestScore,
                    latestMaxScore,
                    latestScorePercent,
                    trend,
                    trendSlope,
                    avgAccuracy
            );

            // ── Study hours this week ──────────────────────────────────────
            List<StudySession> sessions = studySessionRepository
                    .findByStudentIdAndEnrollmentId(studentId, enrollmentId);
            LocalDate weekStart = today.with(DayOfWeek.MONDAY);
            double studyHoursThisWeek = sessions.stream()
                    .filter(s -> s.getDeletedAt() == null)
                    .filter(s -> !s.getSessionDate().isBefore(weekStart))
                    .mapToDouble(s -> s.getDurationMinutes() / 60.0)
                    .sum();

            result.add(new EnrollmentVelocity(
                    enrollmentId,
                    enrollment.getExamName(),
                    enrollment.getExamCode().name(),
                    enrollment.getExamDate(),
                    (int) daysRemaining,
                    syllabusVelocity,
                    mockTestStats,
                    studyHoursThisWeek
            ));
        }

        log.debug("Velocity computed for student={}: {} active enrollments", studentId, result.size());
        return ResponseEntity.ok(new VelocityResponse(result));
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Simple linear regression slope: slope = (n*Σxy - Σx*Σy) / (n*Σx² - (Σx)²)
     * where x = index (0-based) and y = score percent.
     */
    private double linearRegressionSlope(List<Double> values) {
        int n = values.size();
        if (n < 2) return 0.0;

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) return 0.0;
        return (n * sumXY - sumX * sumY) / denominator;
    }
}
