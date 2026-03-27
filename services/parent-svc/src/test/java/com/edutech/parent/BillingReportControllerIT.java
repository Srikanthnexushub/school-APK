package com.edutech.parent;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.CreateParentProfileRequest;
import com.edutech.parent.application.dto.ParentProfileResponse;
import com.edutech.parent.application.dto.StudentFeeReportItem;
import com.edutech.parent.domain.model.FeePayment;
import com.edutech.parent.domain.model.Role;
import com.edutech.parent.domain.model.StudentLink;
import com.edutech.parent.domain.port.out.FeePaymentRepository;
import com.edutech.parent.domain.port.out.StudentLinkRepository;
import com.edutech.parent.infrastructure.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Integration tests for BillingReportController.
 *
 * Strategy:
 *   - RANDOM_PORT boots the full servlet stack against native local Postgres
 *     (env vars: POSTGRES_HOST/PORT/PARENT_SVC_DB_NAME/USERNAME/PASSWORD).
 *   - Native local Kafka (KAFKA_BOOTSTRAP_SERVERS) handles async notification publish.
 *   - JwtTokenValidator is @MockBean — no RSA key required.
 *   - @TestInstance(PER_CLASS) + @BeforeAll: parent profiles are created ONCE via the
 *     REST API so the student_links.parent_id FK is satisfied. Test data uses random
 *     UUIDs for CENTER_ID/STUDENT_IDs, isolating each JVM run from previous runs.
 *   - Tests cover: report retrieval, FULLY_PAID/PARTIAL status derivation, RBAC,
 *     wrong-center 403, fee reminder 204.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BillingReportControllerIT {

    // ---------------------------------------------------------------------------
    // Injected test infrastructure
    // ---------------------------------------------------------------------------

    @Autowired
    TestRestTemplate restTemplate;

    @MockBean
    JwtTokenValidator jwtTokenValidator;

    /** Domain port — backed by the real JPA adapter. Used to seed student links and payments. */
    @Autowired
    StudentLinkRepository studentLinkRepository;

    /** Domain port — backed by the real JPA adapter. Used to seed fee payments. */
    @Autowired
    FeePaymentRepository feePaymentRepository;

    // ---------------------------------------------------------------------------
    // Per-run UUIDs — unique each JVM invocation, preventing cross-run conflicts
    // ---------------------------------------------------------------------------

    private final UUID CENTER_ID        = UUID.randomUUID();
    private final UUID OTHER_CENTER_ID  = UUID.randomUUID();
    private final UUID STUDENT_A_ID     = UUID.randomUUID();
    private final UUID STUDENT_B_ID     = UUID.randomUUID();
    private final UUID PARENT_A_USER_ID = UUID.randomUUID();
    private final UUID PARENT_B_USER_ID = UUID.randomUUID();
    private final UUID ADMIN_USER_ID    = UUID.randomUUID();

    private static final String FAKE_TOKEN = "test-bearer-token";

    // Resolved in @BeforeAll after parent profiles are created via API
    private UUID parentAProfileId;
    private UUID parentBProfileId;

    // ---------------------------------------------------------------------------
    // Principal helpers
    // ---------------------------------------------------------------------------

    private AuthPrincipal centerAdminPrincipal() {
        return new AuthPrincipal(ADMIN_USER_ID, "ca@test.com", Role.CENTER_ADMIN, CENTER_ID, "fp-ca");
    }

    private AuthPrincipal institutionAdminPrincipal() {
        return new AuthPrincipal(UUID.randomUUID(), "ia@test.com", Role.INSTITUTION_ADMIN, null, "fp-ia");
    }

    private AuthPrincipal superAdminPrincipal() {
        return new AuthPrincipal(UUID.randomUUID(), "sa@test.com", Role.SUPER_ADMIN, null, "fp-sa");
    }

    private AuthPrincipal wrongCenterAdminPrincipal() {
        return new AuthPrincipal(UUID.randomUUID(), "other@test.com", Role.CENTER_ADMIN, OTHER_CENTER_ID, "fp-other");
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(FAKE_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpEntity<Void> authEntity() {
        return new HttpEntity<>(authHeaders());
    }

    private void mockAuth(AuthPrincipal principal) {
        given(jwtTokenValidator.validate(anyString())).willReturn(Optional.of(principal));
    }

    // ---------------------------------------------------------------------------
    // One-time test data setup
    // ---------------------------------------------------------------------------

    /**
     * Creates two parent profiles via REST API (satisfying the student_links.parent_id FK),
     * then seeds one student link + one fee payment per student.
     *
     * Student A → CONFIRMED payment → paymentStatus = FULLY_PAID
     * Student B → PENDING payment  → paymentStatus = PARTIAL
     *
     * Runs once per class (@TestInstance PER_CLASS). Unique UUIDs prevent conflicts
     * with records from previous test runs on the same database.
     */
    @BeforeAll
    void seedTestData() {
        // ── Parent A profile ────────────────────────────────────────────────────
        mockAuth(new AuthPrincipal(PARENT_A_USER_ID,
                "parent-a-" + PARENT_A_USER_ID + "@billing.test",
                Role.PARENT, CENTER_ID, "fp-pa"));

        CreateParentProfileRequest reqA = new CreateParentProfileRequest(
                "Parent of Arjun",
                "+910000000001",
                "parent-a-" + PARENT_A_USER_ID + "@billing.test",
                null, null, null, null, null, null,
                "PARENT", null, null);

        ResponseEntity<ParentProfileResponse> profileARsp = restTemplate.exchange(
                "/api/v1/parents", HttpMethod.POST,
                new HttpEntity<>(reqA, authHeaders()), ParentProfileResponse.class);
        parentAProfileId = profileARsp.getBody().id();

        // Student A: ACTIVE link + CONFIRMED payment → FULLY_PAID
        StudentLink linkA = StudentLink.create(
                parentAProfileId, STUDENT_A_ID, "Arjun Kumar", CENTER_ID, "PARENT");
        studentLinkRepository.save(linkA);

        FeePayment paymentA = FeePayment.create(
                parentAProfileId, STUDENT_A_ID, CENTER_ID, null,
                new BigDecimal("15000.00"), "INR", LocalDate.now(),
                "REF-A-" + UUID.randomUUID(), null, "TUITION", "ONLINE");
        paymentA.confirm();
        feePaymentRepository.save(paymentA);

        // ── Parent B profile ────────────────────────────────────────────────────
        mockAuth(new AuthPrincipal(PARENT_B_USER_ID,
                "parent-b-" + PARENT_B_USER_ID + "@billing.test",
                Role.PARENT, CENTER_ID, "fp-pb"));

        CreateParentProfileRequest reqB = new CreateParentProfileRequest(
                "Parent of Priya",
                "+910000000002",
                "parent-b-" + PARENT_B_USER_ID + "@billing.test",
                null, null, null, null, null, null,
                "PARENT", null, null);

        ResponseEntity<ParentProfileResponse> profileBRsp = restTemplate.exchange(
                "/api/v1/parents", HttpMethod.POST,
                new HttpEntity<>(reqB, authHeaders()), ParentProfileResponse.class);
        parentBProfileId = profileBRsp.getBody().id();

        // Student B: ACTIVE link + PENDING payment → PARTIAL
        StudentLink linkB = StudentLink.create(
                parentBProfileId, STUDENT_B_ID, "Priya Sharma", CENTER_ID, "PARENT");
        studentLinkRepository.save(linkB);

        FeePayment paymentB = FeePayment.create(
                parentBProfileId, STUDENT_B_ID, CENTER_ID, null,
                new BigDecimal("5000.00"), "INR", LocalDate.now(),
                "REF-B-" + UUID.randomUUID(), null, "TUITION", "CASH");
        // left as PENDING
        feePaymentRepository.save(paymentB);
    }

    // ---------------------------------------------------------------------------
    // Test 1: CENTER_ADMIN can retrieve billing report for their own center
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("GET /report — CENTER_ADMIN own center → 200 with items")
    void getBillingReport_centerAdmin_ownCenter_returns200() {
        mockAuth(centerAdminPrincipal());

        ResponseEntity<List<StudentFeeReportItem>> response = restTemplate.exchange(
                "/api/v1/parents/billing/report?centerId=" + CENTER_ID,
                HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<StudentFeeReportItem>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isNotEmpty();
    }

    // ---------------------------------------------------------------------------
    // Test 2: CONFIRMED payment → FULLY_PAID status + correct totalPaid
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("GET /report — CONFIRMED payment → FULLY_PAID, totalPaid=15000")
    void getBillingReport_confirmedPayment_fullyPaidStatus() {
        mockAuth(centerAdminPrincipal());

        ResponseEntity<List<StudentFeeReportItem>> response = restTemplate.exchange(
                "/api/v1/parents/billing/report?centerId=" + CENTER_ID,
                HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<StudentFeeReportItem>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StudentFeeReportItem item = response.getBody().stream()
                .filter(i -> STUDENT_A_ID.equals(i.studentId()))
                .findFirst().orElse(null);
        assertThat(item).isNotNull();
        assertThat(item.paymentStatus()).isEqualTo("FULLY_PAID");
        assertThat(item.totalPaid()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(item.studentName()).isEqualTo("Arjun Kumar");
    }

    // ---------------------------------------------------------------------------
    // Test 3: PENDING payment → PARTIAL status
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("GET /report — PENDING payment → PARTIAL status")
    void getBillingReport_pendingPayment_partialStatus() {
        mockAuth(centerAdminPrincipal());

        ResponseEntity<List<StudentFeeReportItem>> response = restTemplate.exchange(
                "/api/v1/parents/billing/report?centerId=" + CENTER_ID,
                HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<StudentFeeReportItem>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StudentFeeReportItem item = response.getBody().stream()
                .filter(i -> STUDENT_B_ID.equals(i.studentId()))
                .findFirst().orElse(null);
        assertThat(item).isNotNull();
        assertThat(item.paymentStatus()).isEqualTo("PARTIAL");
        assertThat(item.studentName()).isEqualTo("Priya Sharma");
    }

    // ---------------------------------------------------------------------------
    // Test 4: CENTER_ADMIN denied for a different center → 403
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("GET /report — CENTER_ADMIN wrong center → 403")
    void getBillingReport_centerAdmin_wrongCenter_returns403() {
        mockAuth(wrongCenterAdminPrincipal());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/parents/billing/report?centerId=" + CENTER_ID,
                HttpMethod.GET, authEntity(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---------------------------------------------------------------------------
    // Test 5: INSTITUTION_ADMIN can query any center → 200
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("GET /report — INSTITUTION_ADMIN any center → 200")
    void getBillingReport_institutionAdmin_anyCenter_returns200() {
        mockAuth(institutionAdminPrincipal());

        ResponseEntity<List<StudentFeeReportItem>> response = restTemplate.exchange(
                "/api/v1/parents/billing/report?centerId=" + CENTER_ID,
                HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<StudentFeeReportItem>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // ---------------------------------------------------------------------------
    // Test 6: SUPER_ADMIN can query any center → 200
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("GET /report — SUPER_ADMIN any center → 200")
    void getBillingReport_superAdmin_anyCenter_returns200() {
        mockAuth(superAdminPrincipal());

        ResponseEntity<List<StudentFeeReportItem>> response = restTemplate.exchange(
                "/api/v1/parents/billing/report?centerId=" + CENTER_ID,
                HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<StudentFeeReportItem>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // ---------------------------------------------------------------------------
    // Test 7: Send reminder for a student → 204 (event published to Kafka async)
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("POST /reminder/{studentId} — CENTER_ADMIN → 204")
    void sendReminder_centerAdmin_returns204() {
        mockAuth(centerAdminPrincipal());

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/parents/billing/reminder/" + STUDENT_B_ID + "?centerId=" + CENTER_ID,
                HttpMethod.POST, authEntity(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ---------------------------------------------------------------------------
    // Test 8: Send reminder — CENTER_ADMIN denied for wrong center → 403
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("POST /reminder/{studentId} — CENTER_ADMIN wrong center → 403")
    void sendReminder_centerAdmin_wrongCenter_returns403() {
        mockAuth(wrongCenterAdminPrincipal());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/parents/billing/reminder/" + STUDENT_B_ID + "?centerId=" + CENTER_ID,
                HttpMethod.POST, authEntity(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
