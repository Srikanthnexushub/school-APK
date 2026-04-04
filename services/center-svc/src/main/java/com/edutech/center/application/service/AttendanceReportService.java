// src/main/java/com/edutech/center/application/service/AttendanceReportService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.*;
import com.edutech.center.application.exception.BatchNotFoundException;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.domain.model.*;
import com.edutech.center.domain.port.out.*;
import com.edutech.center.infrastructure.config.KafkaTopicProperties;
import com.edutech.events.center.AttendanceReportNotifyEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates period-bucketed attendance reports, CSV exports, PDF narrations,
 * AI-powered risk insights, and parent notification dispatch.
 *
 * <p>Period bucketing is pure in-memory over the existing attendance table —
 * no additional DB schema changes required.
 *
 * <p>AI insights are optional and degrade gracefully if ai-gateway is unavailable.
 */
@Service
public class AttendanceReportService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceReportService.class);
    private static final BigDecimal AT_RISK_THRESHOLD = BigDecimal.valueOf(75);

    private final AttendanceRepository     attendanceRepository;
    private final BatchRepository          batchRepository;
    private final BatchMemberRepository    batchMemberRepository;
    private final TeacherRepository        teacherRepository;
    private final RestClient               aiGatewayRestClient;
    private final ObjectMapper             objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties     topicProperties;

    public AttendanceReportService(AttendanceRepository attendanceRepository,
                                   BatchRepository batchRepository,
                                   BatchMemberRepository batchMemberRepository,
                                   TeacherRepository teacherRepository,
                                   RestClient aiGatewayRestClient,
                                   ObjectMapper objectMapper,
                                   KafkaTemplate<String, Object> kafkaTemplate,
                                   KafkaTopicProperties topicProperties) {
        this.attendanceRepository  = attendanceRepository;
        this.batchRepository       = batchRepository;
        this.batchMemberRepository = batchMemberRepository;
        this.teacherRepository     = teacherRepository;
        this.aiGatewayRestClient   = aiGatewayRestClient;
        this.objectMapper          = objectMapper;
        this.kafkaTemplate         = kafkaTemplate;
        this.topicProperties       = topicProperties;
    }

    // ─── Report ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AttendancePeriodReport getReport(UUID centerId, UUID batchId,
                                            AttendancePeriod period,
                                            LocalDate from, LocalDate to,
                                            AuthPrincipal principal) {
        assertAccess(centerId, batchId, principal);
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();
        if (effectiveTo.isBefore(effectiveFrom)) effectiveTo = effectiveFrom;

        List<BatchMember>  members = batchMemberRepository.findActiveByBatchId(batchId);
        List<Attendance>   records = attendanceRepository.findByBatchIdAndDateRange(batchId, effectiveFrom, effectiveTo);

        // Index: studentId → sorted list of attendance records
        Map<UUID, List<Attendance>> byStudent = records.stream()
                .collect(Collectors.groupingBy(Attendance::getStudentId,
                         Collectors.collectingAndThen(Collectors.toList(),
                             list -> { list.sort(Comparator.comparing(Attendance::getDate)); return list; })));

        List<DateRange> bucketRanges = buildBucketRanges(period, effectiveFrom, effectiveTo);

        List<AttendanceBucket> buckets = bucketRanges.stream()
                .map(range -> buildBucket(range, members, records, byStudent))
                .toList();

        // Overall at-risk: any student < 75% across the full range
        List<UUID> atRisk = members.stream()
                .filter(m -> {
                    List<Attendance> all = byStudent.getOrDefault(m.getStudentId(), List.of());
                    return isAtRisk(all);
                })
                .map(BatchMember::getStudentId)
                .toList();

        return new AttendancePeriodReport(batchId, batch.getName(), period,
                effectiveFrom, effectiveTo, buckets, atRisk);
    }

    // ─── AI Insights (on-demand, gracefully degrading) ────────────────────────

    @Transactional(readOnly = true)
    public List<AiAttendanceInsight> getAiInsights(UUID centerId, UUID batchId,
                                                    LocalDate from, LocalDate to,
                                                    AuthPrincipal principal) {
        assertAccess(centerId, batchId, principal);

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();

        List<BatchMember> members = batchMemberRepository.findActiveByBatchId(batchId);
        List<Attendance>  records = attendanceRepository.findByBatchIdAndDateRange(batchId, effectiveFrom, effectiveTo);

        Map<UUID, List<Attendance>> byStudent = records.stream()
                .collect(Collectors.groupingBy(Attendance::getStudentId,
                         Collectors.collectingAndThen(Collectors.toList(),
                             list -> { list.sort(Comparator.comparing(Attendance::getDate)); return list; })));

        return members.stream()
                .map(m -> buildAiInsight(m, byStudent.getOrDefault(m.getStudentId(), List.of()), effectiveFrom, effectiveTo))
                .toList();
    }

    // ─── CSV Export ───────────────────────────────────────────────────────────

    /**
     * Returns CSV bytes for the given period report.
     * Columns: Bucket, StudentId, StudentName, Total, Present, Absent, Late, Excused, %
     */
    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID centerId, UUID batchId,
                             AttendancePeriod period, LocalDate from, LocalDate to,
                             AuthPrincipal principal) {
        AttendancePeriodReport report = getReport(centerId, batchId, period, from, to, principal);
        StringBuilder csv = new StringBuilder();
        csv.append("Bucket,StudentId,StudentName,TotalSessions,Present,Absent,Late,Excused,Attendance%\n");
        for (AttendanceBucket bucket : report.buckets()) {
            for (StudentBucketStat s : bucket.students()) {
                csv.append(csvEscape(bucket.label())).append(',')
                   .append(s.studentId()).append(',')
                   .append(csvEscape(s.studentName())).append(',')
                   .append(s.totalSessions()).append(',')
                   .append(s.present()).append(',')
                   .append(s.absent()).append(',')
                   .append(s.late()).append(',')
                   .append(s.excused()).append(',')
                   .append(s.attendancePercent()).append('\n');
            }
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ─── AI Narration for PDF ─────────────────────────────────────────────────

    /**
     * Calls ai-gateway to generate a 3-5 sentence narrative summary for the attendance report PDF.
     * Gracefully returns a pre-built summary if ai-gateway is unavailable.
     */
    public String generateReportNarration(AttendancePeriodReport report) {
        if (report.buckets().isEmpty()) return "No attendance data recorded for the selected period.";

        // Build stats summary for the prompt
        List<StudentBucketStat> allStats = report.buckets().stream()
                .flatMap(b -> b.students().stream())
                .toList();

        OptionalDouble avgPct = allStats.stream()
                .filter(s -> s.totalSessions() > 0)
                .mapToDouble(s -> s.attendancePercent().doubleValue())
                .average();

        long atRisk = report.atRiskStudentIds().size();

        String prompt = """
                Write a 3-5 sentence professional attendance report narrative for an EduTech platform PDF.
                Batch: %s
                Period: %s to %s (%s)
                Total students: %d
                Average attendance: %.1f%%
                At-risk students (below 75%%): %d
                Buckets: %d

                The narrative should be factual, professional, and suitable for a school attendance report.
                Do not use markdown formatting.
                """.formatted(
                        report.batchName(),
                        report.from(), report.to(), report.period().name(),
                        (long) allStats.stream().mapToDouble(s -> 1).sum() / Math.max(1, report.buckets().size()),
                        avgPct.orElse(0),
                        atRisk,
                        report.buckets().size()
                );

        try {
            Map<String, Object> requestBody = Map.of(
                    "requesterId", "center-svc-attendance-pdf-narration",
                    "systemPrompt", "You are a professional academic attendance report writer. Write clear, concise narrative paragraphs only — no headers, no bullet points, no markdown.",
                    "userMessage", prompt,
                    "maxTokens", 300,
                    "temperature", 0.4
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> response = aiGatewayRestClient.post()
                    .uri("/api/v1/ai/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("content")) {
                String narration = ((String) response.get("content")).strip();
                if (!narration.isBlank()) return narration;
            }
        } catch (Exception e) {
            log.warn("AI narration failed \u2014 using fallback: {}", e.getMessage());
        }

        // Fallback: rule-based narrative
        return String.format(
                "This report covers the attendance records for batch '%s' from %s to %s. " +
                "The overall batch attendance average stands at %.1f%%. " +
                "%d student(s) are currently below the 75%% attendance threshold and require immediate attention. " +
                "Regular monitoring and timely parent communication are recommended to improve overall attendance rates.",
                report.batchName(), report.from(), report.to(),
                avgPct.orElse(0), atRisk
        );
    }

    // ─── Notify Parents ───────────────────────────────────────────────────────

    /**
     * Publishes an AttendanceReportNotifyEvent to center-events so parent-svc
     * can fan out batch attendance summary notifications to all parents.
     */
    @Transactional(readOnly = true)
    public void notifyParents(UUID centerId, UUID batchId, LocalDate from, LocalDate to,
                               AuthPrincipal principal) {
        assertAccess(centerId, batchId, principal);
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();

        List<BatchMember> members = batchMemberRepository.findActiveByBatchId(batchId);
        List<Attendance>  records = attendanceRepository.findByBatchIdAndDateRange(batchId, effectiveFrom, effectiveTo);

        Map<UUID, List<Attendance>> byStudent = records.stream()
                .collect(Collectors.groupingBy(Attendance::getStudentId));

        long atRiskCount = members.stream().filter(m -> {
            List<Attendance> mRecords = byStudent.getOrDefault(m.getStudentId(), List.of());
            return isAtRisk(mRecords);
        }).count();

        double avgPct = members.stream()
                .mapToDouble(m -> {
                    List<Attendance> mr = byStudent.getOrDefault(m.getStudentId(), List.of());
                    if (mr.isEmpty()) return 0;
                    long p = count(mr, AttendanceStatus.PRESENT) + count(mr, AttendanceStatus.LATE);
                    return p * 100.0 / mr.size();
                })
                .average().orElse(0);

        List<UUID> studentIds = members.stream().map(BatchMember::getStudentId).toList();

        AttendanceReportNotifyEvent event = new AttendanceReportNotifyEvent(
                batchId, centerId, batch.getName(),
                effectiveFrom, effectiveTo,
                members.size(), (int) atRiskCount, Math.round(avgPct * 10) / 10.0, studentIds
        );

        kafkaTemplate.send(topicProperties.centerEvents(), event);
        log.info("AttendanceReportNotifyEvent published: batchId={} centerId={} students={} atRisk={}",
                batchId, centerId, members.size(), atRiskCount);
    }

    // ─── Bucket building ──────────────────────────────────────────────────────

    private AttendanceBucket buildBucket(DateRange range, List<BatchMember> members,
                                          List<Attendance> allRecords,
                                          Map<UUID, List<Attendance>> byStudent) {
        // Records within this bucket's date range
        List<Attendance> bucketRecords = allRecords.stream()
                .filter(a -> !a.getDate().isBefore(range.from()) && !a.getDate().isAfter(range.to()))
                .toList();

        Map<UUID, List<Attendance>> bucketByStudent = bucketRecords.stream()
                .collect(Collectors.groupingBy(Attendance::getStudentId));

        List<StudentBucketStat> stats = members.stream()
                .map(m -> computeStudentBucketStat(m, bucketByStudent.getOrDefault(m.getStudentId(), List.of()), byStudent))
                .toList();

        BigDecimal avg = stats.isEmpty() ? BigDecimal.ZERO
                : stats.stream()
                       .map(StudentBucketStat::attendancePercent)
                       .reduce(BigDecimal.ZERO, BigDecimal::add)
                       .divide(BigDecimal.valueOf(stats.size()), 2, RoundingMode.HALF_UP);

        return new AttendanceBucket(range.label(), range.from(), range.to(), stats, avg);
    }

    private StudentBucketStat computeStudentBucketStat(BatchMember member, List<Attendance> bucketRecords,
                                                         Map<UUID, List<Attendance>> allByStudent) {
        long present = count(bucketRecords, AttendanceStatus.PRESENT);
        long absent  = count(bucketRecords, AttendanceStatus.ABSENT);
        long late    = count(bucketRecords, AttendanceStatus.LATE);
        long excused = count(bucketRecords, AttendanceStatus.EXCUSED);
        long total   = bucketRecords.size();

        BigDecimal pct = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf((present + late) * 100.0 / total).setScale(2, RoundingMode.HALF_UP);

        // Streak: computed over ALL records (not just bucket) for continuity
        long streak = computeStreak(allByStudent.getOrDefault(member.getStudentId(), List.of()));

        return new StudentBucketStat(
                member.getStudentId(), member.getStudentName(),
                total, present, absent, late, excused, pct,
                streak, pct.compareTo(AT_RISK_THRESHOLD) < 0 && total > 0);
    }

    /**
     * Counts consecutive present/late days from the most recent session backwards.
     * A streak resets on any ABSENT day.
     */
    private long computeStreak(List<Attendance> sorted) {
        if (sorted.isEmpty()) return 0;
        // sorted ascending by date — iterate from end
        long streak = 0;
        for (int i = sorted.size() - 1; i >= 0; i--) {
            AttendanceStatus s = sorted.get(i).getStatus();
            if (s == AttendanceStatus.PRESENT || s == AttendanceStatus.LATE || s == AttendanceStatus.EXCUSED) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * Composite engagement score 0-100:
     * - Attendance component (0-50 pts): pct * 0.5
     * - Streak component (0-20 pts): min(streak * 2, 20)
     * - Trend component (0-20 pts): 20=improving, 10=stable, 0=declining
     * - Consistency component (0-10 pts): penalised for long absence streaks
     */
    private int computeEngagementScore(List<Attendance> records, LocalDate from, LocalDate to) {
        if (records.isEmpty()) return 0;
        long present = count(records, AttendanceStatus.PRESENT);
        long late    = count(records, AttendanceStatus.LATE);
        long total   = records.size();

        // Attendance component (0–50 pts)
        double pct = (present + late) * 100.0 / total;
        int attendanceScore = (int) Math.min(50, pct * 0.5);

        // Streak component (0–20 pts)
        long streak = computeStreak(records);
        int streakScore = (int) Math.min(20, streak * 2);

        // Trend component (0–20 pts)
        int trendScore = switch (computeTrend(records, from, to)) {
            case "IMPROVING" -> 20;
            case "DECLINING" -> 0;
            default          -> 10;
        };

        // Consistency component (0–10 pts): penalise long absence runs
        long maxAbsentRun = computeMaxAbsentRun(records);
        int consistencyScore = maxAbsentRun > 5 ? 0 : maxAbsentRun > 2 ? 5 : 10;

        return attendanceScore + streakScore + trendScore + consistencyScore;
    }

    private String computeTrend(List<Attendance> records, LocalDate from, LocalDate to) {
        if (records.isEmpty() || from == null || to == null) return "STABLE";
        long totalDays = ChronoUnit.DAYS.between(from, to);
        if (totalDays < 2) return "STABLE";
        LocalDate mid = from.plusDays(totalDays / 2);
        List<Attendance> firstHalf  = records.stream().filter(a -> !a.getDate().isAfter(mid)).toList();
        List<Attendance> secondHalf = records.stream().filter(a -> a.getDate().isAfter(mid)).toList();
        if (firstHalf.isEmpty() || secondHalf.isEmpty()) return "STABLE";
        double firstPct  = (count(firstHalf,  AttendanceStatus.PRESENT) + count(firstHalf,  AttendanceStatus.LATE)) * 100.0 / firstHalf.size();
        double secondPct = (count(secondHalf, AttendanceStatus.PRESENT) + count(secondHalf, AttendanceStatus.LATE)) * 100.0 / secondHalf.size();
        if (secondPct > firstPct + 5) return "IMPROVING";
        if (secondPct < firstPct - 5) return "DECLINING";
        return "STABLE";
    }

    /** Longest consecutive run of ABSENT days. */
    private long computeMaxAbsentRun(List<Attendance> records) {
        long max = 0, cur = 0;
        for (Attendance a : records) {
            if (a.getStatus() == AttendanceStatus.ABSENT) { cur++; max = Math.max(max, cur); }
            else cur = 0;
        }
        return max;
    }

    private boolean isAtRisk(List<Attendance> records) {
        if (records.isEmpty()) return false;
        long present = count(records, AttendanceStatus.PRESENT);
        long late    = count(records, AttendanceStatus.LATE);
        long total   = records.size();
        BigDecimal pct = BigDecimal.valueOf((present + late) * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        return pct.compareTo(AT_RISK_THRESHOLD) < 0;
    }

    private long count(List<Attendance> records, AttendanceStatus status) {
        return records.stream().filter(a -> a.getStatus() == status).count();
    }

    // ─── Period bucket ranges ─────────────────────────────────────────────────

    private List<DateRange> buildBucketRanges(AttendancePeriod period, LocalDate from, LocalDate to) {
        return switch (period) {
            case DAILY     -> buildDaily(from, to);
            case WEEKLY    -> buildWeekly(from, to);
            case MONTHLY   -> buildMonthly(from, to);
            case QUARTERLY -> buildQuarterly(from, to);
            case CUSTOM    -> List.of(new DateRange(from.format(DateTimeFormatter.ISO_DATE)
                                     + " \u2013 " + to.format(DateTimeFormatter.ISO_DATE), from, to));
        };
    }

    private List<DateRange> buildDaily(LocalDate from, LocalDate to) {
        List<DateRange> result = new ArrayList<>();
        LocalDate d = from;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy");
        while (!d.isAfter(to)) {
            result.add(new DateRange(d.format(fmt), d, d));
            d = d.plusDays(1);
        }
        return result;
    }

    private List<DateRange> buildWeekly(LocalDate from, LocalDate to) {
        List<DateRange> result = new ArrayList<>();
        // Start from Monday of the week containing 'from'
        LocalDate weekStart = from.with(WeekFields.ISO.dayOfWeek(), 1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM");
        while (!weekStart.isAfter(to)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDate bucketFrom = weekStart.isBefore(from) ? from : weekStart;
            LocalDate bucketTo   = weekEnd.isAfter(to) ? to : weekEnd;
            String label = "W" + weekStart.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                         + " (" + bucketFrom.format(fmt) + "\u2013" + bucketTo.format(fmt) + ")";
            result.add(new DateRange(label, bucketFrom, bucketTo));
            weekStart = weekStart.plusWeeks(1);
        }
        return result;
    }

    private List<DateRange> buildMonthly(LocalDate from, LocalDate to) {
        List<DateRange> result = new ArrayList<>();
        LocalDate month = from.withDayOfMonth(1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy");
        while (!month.isAfter(to)) {
            LocalDate monthEnd = month.withDayOfMonth(month.lengthOfMonth());
            LocalDate bucketFrom = month.isBefore(from) ? from : month;
            LocalDate bucketTo   = monthEnd.isAfter(to) ? to : monthEnd;
            result.add(new DateRange(month.format(fmt), bucketFrom, bucketTo));
            month = month.plusMonths(1);
        }
        return result;
    }

    private List<DateRange> buildQuarterly(LocalDate from, LocalDate to) {
        List<DateRange> result = new ArrayList<>();
        // Quarters: Q1=Jan-Mar, Q2=Apr-Jun, Q3=Jul-Sep, Q4=Oct-Dec
        int[][] quarters = {{1,3},{4,6},{7,9},{10,12}};
        String[] labels = {"Q1","Q2","Q3","Q4"};

        int startYear = from.getYear();
        int endYear   = to.getYear();

        for (int year = startYear; year <= endYear; year++) {
            for (int q = 0; q < 4; q++) {
                LocalDate qStart = LocalDate.of(year, quarters[q][0], 1);
                LocalDate qEnd   = LocalDate.of(year, quarters[q][1],
                                   Month.of(quarters[q][1]).length(Year.isLeap(year)));
                if (qEnd.isBefore(from) || qStart.isAfter(to)) continue;
                LocalDate bucketFrom = qStart.isBefore(from) ? from : qStart;
                LocalDate bucketTo   = qEnd.isAfter(to) ? to : qEnd;
                String label = labels[q] + " " + year;
                result.add(new DateRange(label, bucketFrom, bucketTo));
            }
        }
        return result;
    }

    // ─── AI insights ─────────────────────────────────────────────────────────

    private AiAttendanceInsight buildAiInsight(BatchMember member, List<Attendance> records,
                                                LocalDate from, LocalDate to) {
        int engagementScore = computeEngagementScore(records, from, to);
        String trend = computeTrend(records, from, to);

        if (records.isEmpty()) {
            return new AiAttendanceInsight(member.getStudentId(), member.getStudentName(),
                    "NONE", "No attendance data in the selected range.", "", BigDecimal.ZERO,
                    engagementScore, trend);
        }

        long present = count(records, AttendanceStatus.PRESENT);
        long late    = count(records, AttendanceStatus.LATE);
        long total   = records.size();
        BigDecimal currentPct = BigDecimal.valueOf((present + late) * 100.0 / total).setScale(1, RoundingMode.HALF_UP);

        // Build pattern string: P=present, A=absent, L=late, E=excused
        String pattern = records.stream()
                .map(r -> switch (r.getStatus()) {
                    case PRESENT -> "P";
                    case ABSENT  -> "A";
                    case LATE    -> "L";
                    case EXCUSED -> "E";
                })
                .collect(Collectors.joining(","));

        String prompt = """
                Student: %s
                Attendance pattern (oldest\u2192newest, P=Present,A=Absent,L=Late,E=Excused): %s
                Current attendance%%: %.1f%%
                Observation date: %s

                Respond ONLY with a valid JSON object (no markdown):
                {
                  "riskLevel": "<HIGH|MEDIUM|LOW|NONE>",
                  "insight": "<1-2 sentence observation about the pattern>",
                  "suggestedAction": "<concise recommended action for teacher/admin>",
                  "predictedEomPercent": <number 0-100, projected end-of-month attendance>
                }
                HIGH if projected < 60%%, MEDIUM if 60-74%%, LOW if 75-84%%, NONE if >= 85%%.
                """.formatted(member.getStudentName(), pattern, currentPct.doubleValue(), to);

        try {
            Map<String, Object> requestBody = Map.of(
                    "requesterId", "center-svc-attendance-insights",
                    "systemPrompt", "You are an expert student attendance analyst for an EduTech platform. Respond only with valid JSON.",
                    "userMessage", prompt,
                    "maxTokens", 200,
                    "temperature", 0.3
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = aiGatewayRestClient.post()
                    .uri("/api/v1/ai/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("content")) {
                return fallbackInsight(member, currentPct, engagementScore, trend);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue((String) response.get("content"), Map.class);
            String riskLevel = String.valueOf(parsed.getOrDefault("riskLevel", "NONE"));
            String insight   = String.valueOf(parsed.getOrDefault("insight", ""));
            String action    = String.valueOf(parsed.getOrDefault("suggestedAction", ""));
            Number predicted = (Number) parsed.getOrDefault("predictedEomPercent", currentPct.doubleValue());

            return new AiAttendanceInsight(member.getStudentId(), member.getStudentName(),
                    riskLevel, insight, action,
                    BigDecimal.valueOf(predicted.doubleValue()).setScale(1, RoundingMode.HALF_UP),
                    engagementScore, trend);

        } catch (Exception e) {
            log.warn("AI insight failed for studentId={} \u2014 graceful degradation: {}", member.getStudentId(), e.getMessage());
            return fallbackInsight(member, currentPct, engagementScore, trend);
        }
    }

    private AiAttendanceInsight fallbackInsight(BatchMember member, BigDecimal pct,
                                                 int engagementScore, String trend) {
        String risk = pct.compareTo(BigDecimal.valueOf(60)) < 0 ? "HIGH"
                    : pct.compareTo(AT_RISK_THRESHOLD) < 0 ? "MEDIUM" : "LOW";
        String insight = "Current attendance is " + pct + "%.";
        String action  = pct.compareTo(AT_RISK_THRESHOLD) < 0
                ? "Parent notification and counseling recommended."
                : "Attendance is acceptable.";
        return new AiAttendanceInsight(member.getStudentId(), member.getStudentName(),
                risk, insight, action, pct, engagementScore, trend);
    }

    // ─── Access control ───────────────────────────────────────────────────────

    private void assertAccess(UUID centerId, UUID batchId, AuthPrincipal principal) {
        if (principal.isSuperAdmin() || principal.isInstitutionAdmin()) return;
        if (principal.belongsToCenter(centerId)) return;
        // TEACHER: JWT centerId null post-approval — verify via DB
        if (principal.isTeacher()) {
            boolean belongs = teacherRepository.findByUserId(principal.userId())
                    .stream().anyMatch(t -> t.getCenterId().equals(centerId));
            if (belongs) return;
        }
        throw new CenterAccessDeniedException();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private record DateRange(String label, LocalDate from, LocalDate to) {}
}
