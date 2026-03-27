package com.edutech.center;

import com.edutech.center.application.dto.AddBatchMemberRequest;
import com.edutech.center.application.dto.AttendanceEntry;
import com.edutech.center.application.dto.AttendanceResponse;
import com.edutech.center.application.dto.AttendanceSummaryResponse;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchMemberResponse;
import com.edutech.center.application.dto.BatchResponse;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.CreateBatchRequest;
import com.edutech.center.application.dto.CreateCenterRequest;
import com.edutech.center.application.dto.MarkAttendanceRequest;
import com.edutech.center.domain.model.AttendanceStatus;
import com.edutech.center.domain.model.Role;
import com.edutech.center.domain.port.out.AiTaggingPort;
import com.edutech.center.domain.port.out.DocumentStoragePort;
import com.edutech.center.infrastructure.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HTTP-level integration tests for attendance marking, batch member management,
 * and attendance summary endpoints.
 *
 * <p>Covers {@link com.edutech.center.api.AttendanceController},
 * {@link com.edutech.center.api.BatchMemberController}, and
 * {@link com.edutech.center.api.AttendanceSummaryController}.
 *
 * <p>Test coverage (10 tests):
 * <ul>
 *   <li>POST /batches/{bId}/members — CENTER_ADMIN adds student → 201</li>
 *   <li>POST /batches/{bId}/members — STUDENT role → 403</li>
 *   <li>GET  /batches/{bId}/members — lists active members</li>
 *   <li>DELETE /batches/{bId}/members/{studentId} — withdraw → 204, absent from list</li>
 *   <li>POST /batches/{bId}/attendance — TEACHER marks attendance → 201</li>
 *   <li>GET  /batches/{bId}/attendance?date=X — returns marked records</li>
 *   <li>POST /batches/{bId}/attendance — re-mark replaces previous records</li>
 *   <li>GET  /batches/{bId}/attendance/summary — TEACHER sees all students with pct</li>
 *   <li>GET  /batches/{bId}/attendance/summary — pct computed correctly (present+late / total)</li>
 *   <li>GET  /batches/{bId}/attendance/my-summary — STUDENT sees only own record</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("center-svc — Attendance & Batch Member Integration Tests")
class AttendanceControllerIT {

    @MockBean
    CenterEventPublisher centerEventPublisher;

    @MockBean
    JwtTokenValidator jwtTokenValidator;

    @MockBean
    DocumentStoragePort documentStoragePort;

    @MockBean
    io.minio.MinioClient minioClient;

    @MockBean
    AiTaggingPort aiTaggingPort;

    @Autowired
    TestRestTemplate restTemplate;

    static final UUID SUPER_ADMIN_ID  = UUID.randomUUID();
    static final UUID CENTER_ADMIN_ID = UUID.randomUUID();
    static final UUID TEACHER_ID      = UUID.randomUUID();
    static final UUID STUDENT1_ID     = UUID.randomUUID();
    static final UUID STUDENT2_ID     = UUID.randomUUID();
    static final UUID OWNER_ID        = UUID.randomUUID();
    static final String FAKE_TOKEN    = "test-token";

    AuthPrincipal superAdminPrincipal;
    AuthPrincipal centerAdminPrincipal;
    AuthPrincipal teacherPrincipal;
    AuthPrincipal student1Principal;

    UUID centerId;
    UUID batchId;

    @BeforeEach
    void setUp() {
        superAdminPrincipal = new AuthPrincipal(SUPER_ADMIN_ID, "sa@test.com", Role.SUPER_ADMIN, null, "fp-sa");
        mockAuth(superAdminPrincipal);
        centerId = createCenter("ATT" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        centerAdminPrincipal = new AuthPrincipal(CENTER_ADMIN_ID, "ca@test.com", Role.CENTER_ADMIN, centerId, "fp-ca");
        teacherPrincipal     = new AuthPrincipal(TEACHER_ID,     "t@test.com",  Role.TEACHER,      centerId, "fp-t");
        student1Principal    = new AuthPrincipal(STUDENT1_ID,    "s1@test.com", Role.STUDENT,      centerId, "fp-s1");

        mockAuth(centerAdminPrincipal);
        batchId = createBatch("Math Batch", "MTH" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());

    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void mockAuth(AuthPrincipal p) {
        when(jwtTokenValidator.validate(anyString())).thenReturn(Optional.of(p));
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(FAKE_TOKEN);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private <T> HttpEntity<T> authEntity(T body) { return new HttpEntity<>(body, authHeaders()); }
    private HttpEntity<Void>  authEntity()        { return new HttpEntity<>(authHeaders()); }

    private UUID createCenter(String code) {
        CreateCenterRequest req = new CreateCenterRequest(
                "Test Center " + code, code,
                "1 Main St", "Bangalore", "Karnataka", "560001",
                "9876543210", code.toLowerCase() + "@test.com",
                null, null, OWNER_ID, null);
        mockAuth(superAdminPrincipal);
        ResponseEntity<CenterResponse> r = restTemplate.exchange(
                "/api/v1/centers", HttpMethod.POST, authEntity(req), CenterResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().id();
    }

    private UUID createBatch(String name, String code) {
        CreateBatchRequest req = new CreateBatchRequest(
                name, code, "Mathematics", null, 40,
                LocalDate.now(), LocalDate.now().plusMonths(6));
        ResponseEntity<BatchResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches",
                HttpMethod.POST, authEntity(req), BatchResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().id();
    }

    private UUID addMember(UUID studentId, String studentName) {
        AddBatchMemberRequest req = new AddBatchMemberRequest(studentId, studentName);
        ResponseEntity<BatchMemberResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches/" + batchId + "/members",
                HttpMethod.POST, authEntity(req), BatchMemberResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().studentId();
    }

    private String membersUrl() {
        return "/api/v1/centers/" + centerId + "/batches/" + batchId + "/members";
    }

    private String attendanceUrl() {
        return "/api/v1/centers/" + centerId + "/batches/" + batchId + "/attendance";
    }

    private String summaryUrl() {
        return attendanceUrl() + "/summary";
    }

    private String mySummaryUrl() {
        return attendanceUrl() + "/my-summary";
    }

    // =========================================================================
    // Test 1: CENTER_ADMIN adds member → 201, studentId stored
    // =========================================================================

    @Test
    @DisplayName("POST /batches/{bId}/members — CENTER_ADMIN adds student → 201, studentId stored")
    void addMember_centerAdmin_succeeds() {
        mockAuth(centerAdminPrincipal);
        UUID studentId = UUID.randomUUID();

        AddBatchMemberRequest req = new AddBatchMemberRequest(studentId, "Ravi Kumar");

        ResponseEntity<BatchMemberResponse> r = restTemplate.exchange(
                membersUrl(), HttpMethod.POST, authEntity(req), BatchMemberResponse.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BatchMemberResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.studentId()).isEqualTo(studentId);
        assertThat(body.studentName()).isEqualTo("Ravi Kumar");
        assertThat(body.batchId()).isEqualTo(batchId);
        assertThat(body.active()).isTrue();
    }

    // =========================================================================
    // Test 2: STUDENT role → 403 when adding member
    // =========================================================================

    @Test
    @DisplayName("POST /batches/{bId}/members — STUDENT role → 403 Forbidden")
    void addMember_student_forbidden() {
        mockAuth(student1Principal);

        AddBatchMemberRequest req = new AddBatchMemberRequest(UUID.randomUUID(), "Hacker Student");

        ResponseEntity<String> r = restTemplate.exchange(
                membersUrl(), HttpMethod.POST, authEntity(req), String.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // Test 3: GET /members — lists active members
    // =========================================================================

    @Test
    @DisplayName("GET /batches/{bId}/members — lists active members after adding")
    void listMembers_returnsAddedStudent() {
        mockAuth(centerAdminPrincipal);
        UUID studentId = UUID.randomUUID();
        addMember(studentId, "Priya Sharma");

        ResponseEntity<List<BatchMemberResponse>> r = restTemplate.exchange(
                membersUrl(), HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<BatchMemberResponse>>() {});

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<UUID> ids = r.getBody().stream().map(BatchMemberResponse::studentId).toList();
        assertThat(ids).contains(studentId);
    }

    // =========================================================================
    // Test 4: DELETE member → 204, absent from active list
    // =========================================================================

    @Test
    @DisplayName("DELETE /batches/{bId}/members/{studentId} — withdraw → 204, absent from list")
    void withdrawMember_removedFromActiveList() {
        mockAuth(centerAdminPrincipal);
        UUID studentId = UUID.randomUUID();
        addMember(studentId, "Withdrawn Student");

        ResponseEntity<Void> del = restTemplate.exchange(
                membersUrl() + "/" + studentId,
                HttpMethod.DELETE, authEntity(), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<List<BatchMemberResponse>> list = restTemplate.exchange(
                membersUrl(), HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<BatchMemberResponse>>() {});

        List<UUID> remaining = list.getBody().stream().map(BatchMemberResponse::studentId).toList();
        assertThat(remaining).doesNotContain(studentId);
    }

    // =========================================================================
    // Test 5: TEACHER marks attendance → 201
    // =========================================================================

    @Test
    @DisplayName("POST /batches/{bId}/attendance — TEACHER marks attendance → 201, records returned")
    void markAttendance_teacher_succeeds() {
        mockAuth(centerAdminPrincipal);
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        addMember(s1, "Student One");
        addMember(s2, "Student Two");

        mockAuth(teacherPrincipal);
        LocalDate today = LocalDate.now();
        MarkAttendanceRequest req = new MarkAttendanceRequest(today, List.of(
                new AttendanceEntry(s1, AttendanceStatus.PRESENT, null),
                new AttendanceEntry(s2, AttendanceStatus.ABSENT,  null)));

        ResponseEntity<List<AttendanceResponse>> r = restTemplate.exchange(
                attendanceUrl(), HttpMethod.POST,
                authEntity(req),
                new ParameterizedTypeReference<List<AttendanceResponse>>() {});

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getBody()).hasSize(2);
        List<AttendanceStatus> statuses = r.getBody().stream()
                .map(AttendanceResponse::status).toList();
        assertThat(statuses).containsExactlyInAnyOrder(AttendanceStatus.PRESENT, AttendanceStatus.ABSENT);
    }

    // =========================================================================
    // Test 6: GET attendance by date — returns marked records
    // =========================================================================

    @Test
    @DisplayName("GET /batches/{bId}/attendance?date=X — returns records for that date")
    void getAttendanceByDate_returnsMarkedRecords() {
        mockAuth(centerAdminPrincipal);
        UUID s1 = UUID.randomUUID();
        addMember(s1, "Query Student");

        mockAuth(teacherPrincipal);
        LocalDate today = LocalDate.now();
        MarkAttendanceRequest req = new MarkAttendanceRequest(today,
                List.of(new AttendanceEntry(s1, AttendanceStatus.LATE, "Arrived 10 min late")));

        restTemplate.exchange(attendanceUrl(), HttpMethod.POST, authEntity(req),
                new ParameterizedTypeReference<List<AttendanceResponse>>() {});

        ResponseEntity<List<AttendanceResponse>> r = restTemplate.exchange(
                attendanceUrl() + "?date=" + today,
                HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<AttendanceResponse>>() {});

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isNotEmpty();
        assertThat(r.getBody().get(0).date()).isEqualTo(today);
    }

    // =========================================================================
    // Test 7: Re-mark attendance replaces previous records
    // =========================================================================

    @Test
    @DisplayName("POST /batches/{bId}/attendance — re-mark same date replaces previous records")
    void markAttendance_remark_replacesPrevious() {
        mockAuth(centerAdminPrincipal);
        UUID s1 = UUID.randomUUID();
        addMember(s1, "Remark Student");

        mockAuth(teacherPrincipal);
        LocalDate today = LocalDate.now();

        // First mark: ABSENT
        restTemplate.exchange(attendanceUrl(), HttpMethod.POST,
                authEntity(new MarkAttendanceRequest(today,
                        List.of(new AttendanceEntry(s1, AttendanceStatus.ABSENT, null)))),
                new ParameterizedTypeReference<List<AttendanceResponse>>() {});

        // Re-mark: PRESENT
        restTemplate.exchange(attendanceUrl(), HttpMethod.POST,
                authEntity(new MarkAttendanceRequest(today,
                        List.of(new AttendanceEntry(s1, AttendanceStatus.PRESENT, null)))),
                new ParameterizedTypeReference<List<AttendanceResponse>>() {});

        // GET should reflect the latest marking
        ResponseEntity<List<AttendanceResponse>> r = restTemplate.exchange(
                attendanceUrl() + "?date=" + today,
                HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<AttendanceResponse>>() {});

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<AttendanceStatus> statuses = r.getBody().stream()
                .map(AttendanceResponse::status).toList();
        // After re-mark, should be PRESENT (no duplicate ABSENT records)
        assertThat(statuses).containsOnly(AttendanceStatus.PRESENT);
    }

    // =========================================================================
    // Test 8: GET summary — TEACHER sees all students with attendance pct
    // =========================================================================

    @Test
    @DisplayName("GET /batches/{bId}/attendance/summary — TEACHER sees all students with pct")
    void getAttendanceSummary_teacher_seesAllStudents() {
        mockAuth(centerAdminPrincipal);
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        addMember(s1, "Summary Student A");
        addMember(s2, "Summary Student B");

        mockAuth(teacherPrincipal);
        // Mark 2 days: s1 present both, s2 present 1 absent 1
        LocalDate d1 = LocalDate.now().minusDays(1);
        LocalDate d2 = LocalDate.now();
        restTemplate.exchange(attendanceUrl(), HttpMethod.POST,
                authEntity(new MarkAttendanceRequest(d1, List.of(
                        new AttendanceEntry(s1, AttendanceStatus.PRESENT, null),
                        new AttendanceEntry(s2, AttendanceStatus.PRESENT, null)))),
                new ParameterizedTypeReference<List<AttendanceResponse>>() {});
        restTemplate.exchange(attendanceUrl(), HttpMethod.POST,
                authEntity(new MarkAttendanceRequest(d2, List.of(
                        new AttendanceEntry(s1, AttendanceStatus.PRESENT, null),
                        new AttendanceEntry(s2, AttendanceStatus.ABSENT,  null)))),
                new ParameterizedTypeReference<List<AttendanceResponse>>() {});

        ResponseEntity<List<AttendanceSummaryResponse>> r = restTemplate.exchange(
                summaryUrl(), HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<AttendanceSummaryResponse>>() {});

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isNotEmpty();
        List<UUID> studentIds = r.getBody().stream().map(AttendanceSummaryResponse::studentId).toList();
        assertThat(studentIds).containsExactlyInAnyOrder(s1, s2);
    }

    // =========================================================================
    // Test 9: Summary pct computed correctly
    // =========================================================================

    @Test
    @DisplayName("GET /batches/{bId}/attendance/summary — pct = (present+late)/total * 100")
    void getAttendanceSummary_pctComputedCorrectly() {
        mockAuth(centerAdminPrincipal);
        UUID s1 = UUID.randomUUID();
        addMember(s1, "Pct Check Student");

        mockAuth(teacherPrincipal);
        // 4 days: 2 PRESENT, 1 LATE, 1 ABSENT → pct = 75.00
        LocalDate base = LocalDate.now().minusDays(3);
        AttendanceStatus[] statuses = {
                AttendanceStatus.PRESENT, AttendanceStatus.LATE,
                AttendanceStatus.PRESENT, AttendanceStatus.ABSENT
        };
        for (int i = 0; i < statuses.length; i++) {
            LocalDate d = base.plusDays(i);
            restTemplate.exchange(attendanceUrl(), HttpMethod.POST,
                    authEntity(new MarkAttendanceRequest(d,
                            List.of(new AttendanceEntry(s1, statuses[i], null)))),
                    new ParameterizedTypeReference<List<AttendanceResponse>>() {});
        }

        ResponseEntity<List<AttendanceSummaryResponse>> r = restTemplate.exchange(
                summaryUrl(), HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<AttendanceSummaryResponse>>() {});

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        AttendanceSummaryResponse rec = r.getBody().stream()
                .filter(s -> s.studentId().equals(s1)).findFirst().orElse(null);
        assertThat(rec).isNotNull();
        assertThat(rec.totalDays()).isEqualTo(4);
        assertThat(rec.presentDays()).isEqualTo(2);
        assertThat(rec.lateDays()).isEqualTo(1);
        assertThat(rec.absentDays()).isEqualTo(1);
        assertThat(rec.attendancePct()).isEqualByComparingTo(new BigDecimal("75.00"));
    }

    // =========================================================================
    // Test 10: STUDENT /my-summary — sees only own record
    // =========================================================================

    @Test
    @DisplayName("GET /batches/{bId}/attendance/my-summary — STUDENT sees only own record")
    void getMyAttendanceSummary_student_seesOnlyOwn() {
        mockAuth(centerAdminPrincipal);
        // Register STUDENT1_ID as member
        addMember(STUDENT1_ID, "My Student");
        UUID otherId = UUID.randomUUID();
        addMember(otherId, "Other Student");

        mockAuth(teacherPrincipal);
        LocalDate today = LocalDate.now();
        restTemplate.exchange(attendanceUrl(), HttpMethod.POST,
                authEntity(new MarkAttendanceRequest(today, List.of(
                        new AttendanceEntry(STUDENT1_ID, AttendanceStatus.PRESENT, null),
                        new AttendanceEntry(otherId,     AttendanceStatus.PRESENT, null)))),
                new ParameterizedTypeReference<List<AttendanceResponse>>() {});

        mockAuth(student1Principal);
        ResponseEntity<List<AttendanceSummaryResponse>> r = restTemplate.exchange(
                mySummaryUrl(), HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<AttendanceSummaryResponse>>() {});

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isNotEmpty();
        // All records must belong to the authenticated student
        r.getBody().forEach(s -> assertThat(s.studentId()).isEqualTo(STUDENT1_ID));
        // The other student's record must not appear
        List<UUID> ids = r.getBody().stream().map(AttendanceSummaryResponse::studentId).toList();
        assertThat(ids).doesNotContain(otherId);
    }
}
