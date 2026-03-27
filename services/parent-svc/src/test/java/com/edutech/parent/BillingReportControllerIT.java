package com.edutech.parent;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.StudentFeeReportItem;
import com.edutech.parent.domain.model.FeePayment;
import com.edutech.parent.domain.model.Role;
import com.edutech.parent.domain.model.StudentLink;
import com.edutech.parent.domain.port.out.FeePaymentRepository;
import com.edutech.parent.domain.port.out.StudentLinkRepository;
import com.edutech.parent.infrastructure.security.JwtTokenValidator;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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
 *   - RANDOM_PORT boots the full servlet stack with real DB (PostgreSQLContainer).
 *   - KafkaContainer satisfies spring-kafka auto-configuration (reminder publishes via Kafka).
 *   - JwtTokenValidator is @MockBean for principal injection without RSA key.
 *   - Domain port interfaces (FeePaymentRepository, StudentLinkRepository) used for test data
 *     seeding — avoids coupling to package-private Spring Data repositories.
 *   - Tests cover: report retrieval, status derivation, RBAC, reminder 204, wrong-center 403.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class BillingReportControllerIT {

    // ---------------------------------------------------------------------------
    // Infrastructure containers
    // ---------------------------------------------------------------------------

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("parent_db_billing_it")
                    .withUsername("parent_user")
                    .withPassword("parent_pass");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    // ---------------------------------------------------------------------------
    // Injected test infrastructure
    // ---------------------------------------------------------------------------

    @Autowired
    TestRestTemplate restTemplate;

    @MockBean
    JwtTokenValidator jwtTokenValidator;

    /** Domain port — backed by the real JPA adapter. Used to seed test data. */
    @Autowired
    StudentLinkRepository studentLinkRepository;

    /** Domain port — backed by the real JPA adapter. Used to seed test data. */
    @Autowired
    FeePaymentRepository feePaymentRepository;

    // ---------------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------------

    private static final UUID CENTER_ID       = UUID.randomUUID();
    private static final UUID OTHER_CENTER_ID = UUID.randomUUID();
    private static final UUID STUDENT_A_ID    = UUID.randomUUID();
    private static final UUID STUDENT_B_ID    = UUID.randomUUID();
    private static final UUID PARENT_A_ID     = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID   = UUID.randomUUID();

    private static final String FAKE_TOKEN = "test-bearer-token";

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
    // Test data setup
    // ---------------------------------------------------------------------------

    @BeforeEach
    void seedTestData() {
        // Student A: ACTIVE link + one CONFIRMED payment → FULLY_PAID
        StudentLink linkA = StudentLink.create(PARENT_A_ID, STUDENT_A_ID, "Arjun Kumar", CENTER_ID, "PARENT");
        studentLinkRepository.save(linkA);

        FeePayment paymentA = FeePayment.create(
                PARENT_A_ID, STUDENT_A_ID, CENTER_ID, null,
                new BigDecimal("15000.00"), "INR", LocalDate.now(),
                "REF-A-001-" + UUID.randomUUID(), null, "TUITION", "ONLINE"
        );
        paymentA.confirm();
        feePaymentRepository.save(paymentA);

        // Student B: ACTIVE link + one PENDING payment → PARTIAL
        UUID parentBId = UUID.randomUUID();
        StudentLink linkB = StudentLink.create(parentBId, STUDENT_B_ID, "Priya Sharma", CENTER_ID, "PARENT");
        studentLinkRepository.save(linkB);

        FeePayment paymentB = FeePayment.create(
                parentBId, STUDENT_B_ID, CENTER_ID, null,
                new BigDecimal("5000.00"), "INR", LocalDate.now(),
                "REF-B-001-" + UUID.randomUUID(), null, "TUITION", "CASH"
        );
        // leave as PENDING — status will be PARTIAL
        feePaymentRepository.save(paymentB);
    }

    // ---------------------------------------------------------------------------
    // Test 1: CENTER_ADMIN can retrieve billing report for their own center
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/parents/billing/report — CENTER_ADMIN own center → 200 with items")
    void getBillingReport_centerAdmin_ownCenter_returns200() {
        mockAuth(centerAdminPrincipal());

        ResponseEntity<List<StudentFeeReportItem>> response = restTemplate.exchange(
                "/api/v1/parents/billing/report?centerId=" + CENTER_ID,
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<StudentFeeReportItem>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<StudentFeeReportItem> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).isNotEmpty();
    }

    // ---------------------------------------------------------------------------
    // Test 2: CONFIRMED payment → FULLY_PAID status in report
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/parents/billing/report — CONFIRMED payment → FULLY_PAID status")
    void getBillingReport_confirmedPayment_fullyPaidStatus() {
        mockAuth(centerAdminPrincipal());

        ResponseEntity<List<StudentFeeReportItem>> response = restTemplate.exchange(
                "/api/v1/parents/billing/report?centerId=" + CENTER_ID,
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<StudentFeeReportItem>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<StudentFeeReportItem> body = response.getBody();
        assertThat(body).isNotNull();

        StudentFeeReportItem item = body.stream()
                .filter(i -> STUDENT_A_ID.equals(i.studentId()))
                .findFirst()
                .orElse(null);
        assertThat(item).isNotNull();
        assertThat(item.paymentStatus()).isEqualTo("FULLY_PAID");
        assertThat(item.totalPaid()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(item.studentName()).isEqualTo("Arjun Kumar");
    }

    // ---------------------------------------------------------------------------
    // Test 3: PENDING payment → PARTIAL status in report
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/parents/billing/report — PENDING payment → PARTIAL status")
    void getBillingReport_pendingPayment_partialStatus() {
        mockAuth(centerAdminPrincipal());

        ResponseEntity<List<StudentFeeReportItem>> response = restTemplate.exchange(
                "/api/v1/parents/billing/report?centerId=" + CENTER_ID,
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<StudentFeeReportItem>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<StudentFeeReportItem> body = response.getBody();
        assertThat(body).isNotNull();

        StudentFeeReportItem item = body.stream()
                .filter(i -> STUDENT_B_ID.equals(i.studentId()))
                .findFirst()
                .orElse(null);
        assertThat(item).isNotNull();
        assertThat(item.paymentStatus()).isEqualTo("PARTIAL");
        assertThat(item.studentName()).isEqualTo("Priya Sharma");
    }

    // ---------------------------------------------------------------------------
    // Test 4: CENTER_ADMIN denied for a different center → 403
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/parents/billing/report — CENTER_ADMIN wrong center → 403")
    void getBillingReport_centerAdmin_wrongCenter_returns403() {
        mockAuth(wrongCenterAdminPrincipal());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/parents/billing/report?centerId=" + CENTER_ID,
                HttpMethod.GET,
                authEntity(),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---------------------------------------------------------------------------
    // Test 5: INSTITUTION_ADMIN can query any center → 200
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/parents/billing/report — INSTITUTION_ADMIN any center → 200")
    void getBillingReport_institutionAdmin_anyCenter_returns200() {
        mockAuth(institutionAdminPrincipal());

        ResponseEntity<List<StudentFeeReportItem>> response = restTemplate.exchange(
                "/api/v1/parents/billing/report?centerId=" + CENTER_ID,
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<StudentFeeReportItem>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // ---------------------------------------------------------------------------
    // Test 6: SUPER_ADMIN can query any center → 200
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/parents/billing/report — SUPER_ADMIN any center → 200")
    void getBillingReport_superAdmin_anyCenter_returns200() {
        mockAuth(superAdminPrincipal());

        ResponseEntity<List<StudentFeeReportItem>> response = restTemplate.exchange(
                "/api/v1/parents/billing/report?centerId=" + CENTER_ID,
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<StudentFeeReportItem>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // ---------------------------------------------------------------------------
    // Test 7: Send fee reminder → 204 (event published to Kafka asynchronously)
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/parents/billing/reminder/{studentId} — CENTER_ADMIN → 204")
    void sendReminder_centerAdmin_returns204() {
        mockAuth(centerAdminPrincipal());

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/parents/billing/reminder/" + STUDENT_B_ID + "?centerId=" + CENTER_ID,
                HttpMethod.POST,
                authEntity(),
                Void.class
        );

        // Notification dispatched asynchronously via Kafka; controller returns 204 immediately
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ---------------------------------------------------------------------------
    // Test 8: Send reminder — CENTER_ADMIN denied for wrong center → 403
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/parents/billing/reminder/{studentId} — CENTER_ADMIN wrong center → 403")
    void sendReminder_centerAdmin_wrongCenter_returns403() {
        mockAuth(wrongCenterAdminPrincipal());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/parents/billing/reminder/" + STUDENT_B_ID + "?centerId=" + CENTER_ID,
                HttpMethod.POST,
                authEntity(),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
