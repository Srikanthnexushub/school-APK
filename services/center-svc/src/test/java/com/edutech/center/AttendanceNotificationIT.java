package com.edutech.center;

import com.edutech.center.application.dto.*;
import com.edutech.center.application.service.AttendanceService;
import com.edutech.center.application.service.BatchMemberService;
import com.edutech.center.domain.model.AttendanceStatus;
import com.edutech.center.domain.model.BatchMode;
import com.edutech.center.domain.model.Role;
import com.edutech.center.domain.port.out.AiTaggingPort;
import com.edutech.center.domain.port.out.CenterEventPublisher;
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
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for attendance marking with Kafka notification path.
 *
 * <p>Kafka is configured to point at a non-existent broker (localhost:9999).
 * Notification publishes are best-effort fire-and-forget; exceptions are swallowed,
 * so the main attendance save path must still succeed.
 *
 * <p>Test coverage:
 * <ul>
 *   <li>POST /batches/{bId}/attendance — saved records returned even when Kafka is down</li>
 *   <li>POST /batches/{bId}/attendance — TEACHER with matching centerId in JWT can mark</li>
 *   <li>POST /batches/{bId}/attendance — PARENT role receives 403 (no access)</li>
 *   <li>GET  /batches/{bId}/attendance/report — MONTHLY period report returns correct structure</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("center-svc — Attendance Notification + Report Integration Tests")
class AttendanceNotificationIT {

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
    static final UUID STUDENT_ID      = UUID.randomUUID();
    static final UUID PARENT_ID       = UUID.randomUUID();
    static final UUID OWNER_ID        = UUID.randomUUID();
    static final String FAKE_TOKEN    = "test-token";

    AuthPrincipal superAdminPrincipal;
    AuthPrincipal centerAdminPrincipal;
    AuthPrincipal teacherPrincipal;
    AuthPrincipal parentPrincipal;

    UUID centerId;
    UUID batchId;

    @BeforeEach
    void setUp() {
        superAdminPrincipal = new AuthPrincipal(SUPER_ADMIN_ID, "sa@test.com", Role.SUPER_ADMIN, null, "fp-sa");
        mockAuth(superAdminPrincipal);
        centerId = createCenter("NTF" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        centerAdminPrincipal = new AuthPrincipal(CENTER_ADMIN_ID, "ca@test.com", Role.CENTER_ADMIN, centerId, "fp-ca");
        teacherPrincipal     = new AuthPrincipal(TEACHER_ID,     "t@test.com",  Role.TEACHER,      centerId, "fp-t");
        parentPrincipal      = new AuthPrincipal(PARENT_ID,      "p@test.com",  Role.PARENT,       null,     "fp-p");

        mockAuth(centerAdminPrincipal);
        batchId = createBatch("Notification Batch", "NTF" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        addMember(STUDENT_ID, "Test Student");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

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
                "Notification Center " + code, code,
                "1 Notif St", "Bangalore", "Karnataka", "560001",
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
                name, code, "Science", null, 30,
                LocalDate.now(), LocalDate.now().plusMonths(6), BatchMode.OFFLINE);
        ResponseEntity<BatchResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches",
                HttpMethod.POST, authEntity(req), BatchResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().id();
    }

    private void addMember(UUID studentId, String studentName) {
        AddBatchMemberRequest req = new AddBatchMemberRequest(studentId, studentName);
        ResponseEntity<BatchMemberResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches/" + batchId + "/members",
                HttpMethod.POST, authEntity(req), BatchMemberResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private String attendanceUrl() {
        return "/api/v1/centers/" + centerId + "/batches/" + batchId + "/attendance";
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST attendance — saved records returned even when Kafka broker is unavailable")
    void markAttendance_kafkaDown_recordsSavedSuccessfully() {
        mockAuth(teacherPrincipal);
        LocalDate today = LocalDate.now();
        MarkAttendanceRequest req = new MarkAttendanceRequest(today, List.of(
                new AttendanceEntry(STUDENT_ID, AttendanceStatus.ABSENT, "Test notification path")));

        ResponseEntity<List<AttendanceResponse>> r = restTemplate.exchange(
                attendanceUrl(), HttpMethod.POST,
                authEntity(req),
                new ParameterizedTypeReference<List<AttendanceResponse>>() {});

        // Attendance must be saved even though Kafka publish will fail (non-existent broker)
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getBody()).hasSize(1);
        assertThat(r.getBody().get(0).status()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(r.getBody().get(0).studentId()).isEqualTo(STUDENT_ID);
    }

    @Test
    @DisplayName("POST attendance — TEACHER with centerId in JWT can mark attendance")
    void markAttendance_teacherWithCenterId_succeeds() {
        mockAuth(teacherPrincipal);
        LocalDate today = LocalDate.now();
        MarkAttendanceRequest req = new MarkAttendanceRequest(today, List.of(
                new AttendanceEntry(STUDENT_ID, AttendanceStatus.PRESENT, null)));

        ResponseEntity<List<AttendanceResponse>> r = restTemplate.exchange(
                attendanceUrl(), HttpMethod.POST,
                authEntity(req),
                new ParameterizedTypeReference<List<AttendanceResponse>>() {});

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getBody()).hasSize(1);
        assertThat(r.getBody().get(0).status()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("POST attendance — PARENT role receives 403 Forbidden")
    void markAttendance_parentRole_forbidden() {
        mockAuth(parentPrincipal);
        LocalDate today = LocalDate.now();
        MarkAttendanceRequest req = new MarkAttendanceRequest(today, List.of(
                new AttendanceEntry(STUDENT_ID, AttendanceStatus.PRESENT, null)));

        ResponseEntity<String> r = restTemplate.exchange(
                attendanceUrl(), HttpMethod.POST, authEntity(req), String.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /report?period=MONTHLY — returns one bucket for single-month range")
    void reportEndpoint_monthlyPeriod_returnsBucket() {
        // Mark some attendance first
        mockAuth(teacherPrincipal);
        LocalDate today = LocalDate.now();
        restTemplate.exchange(attendanceUrl(), HttpMethod.POST,
                authEntity(new MarkAttendanceRequest(today, List.of(
                        new AttendanceEntry(STUDENT_ID, AttendanceStatus.PRESENT, null)))),
                new ParameterizedTypeReference<List<AttendanceResponse>>() {});

        // Now call the report endpoint
        mockAuth(teacherPrincipal);
        LocalDate from = today.withDayOfMonth(1);
        LocalDate to   = today;
        String url = attendanceUrl() + "/report?period=MONTHLY&from=" + from + "&to=" + to;

        ResponseEntity<AttendancePeriodReport> r = restTemplate.exchange(
                url, HttpMethod.GET, authEntity(), AttendancePeriodReport.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        AttendancePeriodReport report = r.getBody();
        assertThat(report).isNotNull();
        assertThat(report.batchId()).isEqualTo(batchId);
        assertThat(report.period()).isEqualTo(com.edutech.center.domain.model.AttendancePeriod.MONTHLY);
        assertThat(report.buckets()).isNotEmpty();
    }
}
