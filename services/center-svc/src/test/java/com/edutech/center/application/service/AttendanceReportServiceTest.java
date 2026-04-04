package com.edutech.center.application.service;

import com.edutech.center.application.dto.*;
import com.edutech.center.domain.model.*;
import com.edutech.center.domain.port.out.*;
import com.edutech.center.infrastructure.config.KafkaTopicProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for {@link AttendanceReportService}.
 * No Spring context — Mockito only.
 *
 * <p>LENIENT strictness used because the shared {@code batch} mock stubs getId/getCenterId/getName
 * which are not all consumed by every test (e.g. quarterly/custom with empty members).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendanceReportServiceTest {

    @Mock AttendanceRepository              attendanceRepository;
    @Mock BatchRepository                   batchRepository;
    @Mock BatchMemberRepository             batchMemberRepository;
    @Mock TeacherRepository                 teacherRepository;
    @Mock RestClient                        aiGatewayRestClient;
    @Mock ObjectMapper                      objectMapper;
    @Mock KafkaTemplate<String, Object>     kafkaTemplate;
    @Mock KafkaTopicProperties              topicProperties;

    AttendanceReportService service;

    private final UUID centerId = UUID.randomUUID();
    private final UUID batchId  = UUID.randomUUID();
    private final UUID studentA = UUID.randomUUID();
    private final UUID studentB = UUID.randomUUID();

    /**
     * All mocks are pre-stubbed in setUp() so that stubs are fully initialized before
     * any when().thenReturn() call in individual tests. This avoids Mockito's
     * UnfinishedStubbingException which occurs when mock() + when() appear nested inside
     * the parameter list of another when().thenReturn().
     */
    private Batch batch;
    private BatchMember memberA;
    private BatchMember memberB;
    private AuthPrincipal admin;

    @BeforeEach
    void setUp() {
        service = new AttendanceReportService(
                attendanceRepository, batchRepository, batchMemberRepository,
                teacherRepository, aiGatewayRestClient, objectMapper,
                kafkaTemplate, topicProperties);

        batch = mock(Batch.class);
        when(batch.getId()).thenReturn(batchId);
        when(batch.getCenterId()).thenReturn(centerId);
        when(batch.getName()).thenReturn("Class 10-A");

        memberA = mock(BatchMember.class);
        when(memberA.getStudentId()).thenReturn(studentA);
        when(memberA.getStudentName()).thenReturn("Alice");

        memberB = mock(BatchMember.class);
        when(memberB.getStudentId()).thenReturn(studentB);
        when(memberB.getStudentName()).thenReturn("Bob");

        admin = mock(AuthPrincipal.class);
        when(admin.isSuperAdmin()).thenReturn(true);
    }

    /** Build an Attendance mock. Safe to call ONLY when the result is assigned to a local variable first. */
    private Attendance attend(UUID studentId, LocalDate date, AttendanceStatus status) {
        Attendance a = mock(Attendance.class);
        when(a.getStudentId()).thenReturn(studentId);
        when(a.getDate()).thenReturn(date);
        when(a.getStatus()).thenReturn(status);
        return a;
    }

    @Test
    @DisplayName("MONTHLY report: single month produces one bucket with correct stats")
    void monthlyReport_singleMonth_correctBucket() {
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(batchMemberRepository.findActiveByBatchId(batchId)).thenReturn(List.of(memberA, memberB));

        LocalDate apr1 = LocalDate.of(2026, 4, 1);
        LocalDate apr5 = LocalDate.of(2026, 4, 5);
        // Alice: 4 present, 1 absent -> 80%
        // Bob:   2 present, 3 absent -> 40% (at risk)
        List<Attendance> records = new ArrayList<>(List.of(
                attend(studentA, apr1, AttendanceStatus.PRESENT),
                attend(studentA, apr1.plusDays(1), AttendanceStatus.PRESENT),
                attend(studentA, apr1.plusDays(2), AttendanceStatus.PRESENT),
                attend(studentA, apr1.plusDays(3), AttendanceStatus.PRESENT),
                attend(studentA, apr5, AttendanceStatus.ABSENT),
                attend(studentB, apr1, AttendanceStatus.PRESENT),
                attend(studentB, apr1.plusDays(1), AttendanceStatus.PRESENT),
                attend(studentB, apr1.plusDays(2), AttendanceStatus.ABSENT),
                attend(studentB, apr1.plusDays(3), AttendanceStatus.ABSENT),
                attend(studentB, apr5, AttendanceStatus.ABSENT)
        ));
        when(attendanceRepository.findByBatchIdAndDateRange(batchId, apr1, apr5)).thenReturn(records);

        AttendancePeriodReport report = service.getReport(centerId, batchId,
                AttendancePeriod.MONTHLY, apr1, apr5, admin);

        assertThat(report.buckets()).hasSize(1);
        AttendanceBucket bucket = report.buckets().get(0);
        assertThat(bucket.label()).isEqualTo("April 2026");

        StudentBucketStat alice = bucket.students().stream()
                .filter(s -> s.studentId().equals(studentA)).findFirst().orElseThrow();
        assertThat(alice.present()).isEqualTo(4);
        assertThat(alice.absent()).isEqualTo(1);
        assertThat(alice.attendancePercent()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(alice.atRisk()).isFalse();

        StudentBucketStat bob = bucket.students().stream()
                .filter(s -> s.studentId().equals(studentB)).findFirst().orElseThrow();
        assertThat(bob.present()).isEqualTo(2);
        assertThat(bob.absent()).isEqualTo(3);
        assertThat(bob.attendancePercent()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(bob.atRisk()).isTrue();

        assertThat(report.atRiskStudentIds()).containsExactly(studentB);
    }

    @Test
    @DisplayName("WEEKLY report: date range spanning 2 weeks produces 2 buckets")
    void weeklyReport_twoWeeks_twoBuckets() {
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(batchMemberRepository.findActiveByBatchId(batchId)).thenReturn(List.of(memberA));

        LocalDate from = LocalDate.of(2026, 4, 7); // Monday
        LocalDate to   = LocalDate.of(2026, 4, 14); // Tuesday next week

        // Pre-build attendance mocks BEFORE passing to when().thenReturn() to avoid UnfinishedStubbingException
        Attendance w1 = attend(studentA, from, AttendanceStatus.PRESENT);
        Attendance w2 = attend(studentA, from.plusDays(7), AttendanceStatus.ABSENT);
        when(attendanceRepository.findByBatchIdAndDateRange(batchId, from, to))
                .thenReturn(new ArrayList<>(List.of(w1, w2)));

        AttendancePeriodReport report = service.getReport(centerId, batchId,
                AttendancePeriod.WEEKLY, from, to, admin);

        assertThat(report.buckets()).hasSize(2);
    }

    @Test
    @DisplayName("QUARTERLY report: Q1 and Q2 2026 produces 2 buckets")
    void quarterlyReport_q1q2_twoBuckets() {
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(batchMemberRepository.findActiveByBatchId(batchId)).thenReturn(List.of());

        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to   = LocalDate.of(2026, 6, 30);
        when(attendanceRepository.findByBatchIdAndDateRange(batchId, from, to)).thenReturn(List.of());

        AttendancePeriodReport report = service.getReport(centerId, batchId,
                AttendancePeriod.QUARTERLY, from, to, admin);

        assertThat(report.buckets()).hasSize(2);
        assertThat(report.buckets().get(0).label()).isEqualTo("Q1 2026");
        assertThat(report.buckets().get(1).label()).isEqualTo("Q2 2026");
    }

    @Test
    @DisplayName("CUSTOM report: single bucket covering entire range")
    void customReport_singleBucket() {
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(batchMemberRepository.findActiveByBatchId(batchId)).thenReturn(List.of());

        LocalDate from = LocalDate.of(2026, 3, 10);
        LocalDate to   = LocalDate.of(2026, 3, 20);
        when(attendanceRepository.findByBatchIdAndDateRange(batchId, from, to)).thenReturn(List.of());

        AttendancePeriodReport report = service.getReport(centerId, batchId,
                AttendancePeriod.CUSTOM, from, to, admin);

        assertThat(report.buckets()).hasSize(1);
        assertThat(report.buckets().get(0).from()).isEqualTo(from);
        assertThat(report.buckets().get(0).to()).isEqualTo(to);
    }

    @Test
    @DisplayName("Streak: consecutive present days from end counted correctly")
    void streak_consecutivePresentFromEnd() {
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(batchMemberRepository.findActiveByBatchId(batchId)).thenReturn(List.of(memberA));

        LocalDate base = LocalDate.of(2026, 4, 1);
        // Pattern: ABSENT, PRESENT, PRESENT, PRESENT -> streak = 3
        List<Attendance> records = new ArrayList<>(List.of(
                attend(studentA, base,             AttendanceStatus.ABSENT),
                attend(studentA, base.plusDays(1), AttendanceStatus.PRESENT),
                attend(studentA, base.plusDays(2), AttendanceStatus.PRESENT),
                attend(studentA, base.plusDays(3), AttendanceStatus.PRESENT)
        ));
        when(attendanceRepository.findByBatchIdAndDateRange(batchId, base, base.plusDays(3)))
                .thenReturn(records);

        AttendancePeriodReport report = service.getReport(centerId, batchId,
                AttendancePeriod.CUSTOM, base, base.plusDays(3), admin);

        StudentBucketStat alice = report.buckets().get(0).students().get(0);
        assertThat(alice.streak()).isEqualTo(3);
    }

    @Test
    @DisplayName("at-risk flag: student with 0 sessions is NOT flagged")
    void atRisk_noSessions_notFlagged() {
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(batchMemberRepository.findActiveByBatchId(batchId)).thenReturn(List.of(memberA));

        LocalDate date = LocalDate.of(2026, 4, 1);
        when(attendanceRepository.findByBatchIdAndDateRange(batchId, date, date)).thenReturn(List.of());

        AttendancePeriodReport report = service.getReport(centerId, batchId,
                AttendancePeriod.CUSTOM, date, date, admin);

        assertThat(report.atRiskStudentIds()).isEmpty();
    }

    @Test
    @DisplayName("CSV export: header + data rows, correct format")
    void csvExport_correctFormat() {
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(batchMemberRepository.findActiveByBatchId(batchId)).thenReturn(List.of(memberA));

        LocalDate date = LocalDate.of(2026, 4, 1);

        // Pre-build attendance mock BEFORE passing to when().thenReturn()
        Attendance a1 = attend(studentA, date, AttendanceStatus.PRESENT);
        when(attendanceRepository.findByBatchIdAndDateRange(batchId, date, date))
                .thenReturn(new ArrayList<>(List.of(a1)));

        byte[] csv = service.exportCsv(centerId, batchId, AttendancePeriod.CUSTOM, date, date, admin);
        String content = new String(csv, java.nio.charset.StandardCharsets.UTF_8);

        assertThat(content).startsWith("Bucket,StudentId,StudentName,TotalSessions,Present,Absent,Late,Excused,Attendance%");
        assertThat(content).contains("Alice");
        assertThat(content).contains("100.00");
    }
}
