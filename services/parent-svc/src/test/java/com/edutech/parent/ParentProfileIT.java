package com.edutech.parent;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.CreateParentProfileRequest;
import com.edutech.parent.application.dto.LinkStudentRequest;
import com.edutech.parent.application.dto.RecordFeePaymentRequest;
import com.edutech.parent.application.service.FeePaymentService;
import com.edutech.parent.application.service.ParentProfileService;
import com.edutech.parent.application.service.StudentLinkService;
import com.edutech.parent.domain.model.LinkStatus;
import com.edutech.parent.domain.model.PaymentStatus;
import com.edutech.parent.domain.model.Role;
import com.edutech.parent.infrastructure.security.JwtTokenValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for parent-svc.
 *
 * Strategy:
 *   - PostgreSQLContainer runs Flyway migrations against a real DB.
 *   - KafkaContainer satisfies the spring-kafka auto-configuration.
 *   - JwtTokenValidator is @MockBean so the @PostConstruct key-load is skipped.
 *   - Redis auto-configuration is excluded in application-test.yml.
 *   - Tests exercise the full Spring stack (service → repository → DB) without HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ParentProfileIT {

    // ---------------------------------------------------------------------------
    // Infrastructure containers
    // ---------------------------------------------------------------------------

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("parent_db_test")
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
    // Beans under test
    // ---------------------------------------------------------------------------

    @Autowired
    ParentProfileService parentProfileService;

    @Autowired
    StudentLinkService studentLinkService;

    @Autowired
    FeePaymentService feePaymentService;

    /**
     * The real JwtTokenValidator reads an RSA key from disk at @PostConstruct.
     * Replace it with a no-op mock so the context loads cleanly.
     */
    @MockBean
    JwtTokenValidator jwtTokenValidator;

    // ---------------------------------------------------------------------------
    // Test fixtures
    // ---------------------------------------------------------------------------

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID CENTER_ID  = UUID.randomUUID();

    /** A principal that owns any profile whose userId matches USER_ID. */
    private AuthPrincipal ownerPrincipal() {
        return new AuthPrincipal(USER_ID, "parent@test.com", Role.PARENT, CENTER_ID, "fp-test");
    }

    /** A SUPER_ADMIN principal that passes all ownsProfile checks. */
    private AuthPrincipal adminPrincipal() {
        return new AuthPrincipal(UUID.randomUUID(), "admin@test.com", Role.SUPER_ADMIN, null, "fp-admin");
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("createParentProfile_persistsToDatabase: saved profile is retrievable by userId")
    void createParentProfile_persistsToDatabase() {
        CreateParentProfileRequest request = new CreateParentProfileRequest(
                "Suresh Verma",
                "+919964636666",
                "suresh@test.com",
                "12 Main Street",
                "Bengaluru",
                null,
                "Karnataka",
                null,
                "560001",
                "PARENT",
                null,
                null
        );

        var response = parentProfileService.createProfile(request, ownerPrincipal());

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.name()).isEqualTo("Suresh Verma");
        assertThat(response.phone()).isEqualTo("+919964636666");
        assertThat(response.email()).isEqualTo("suresh@test.com");
        assertThat(response.city()).isEqualTo("Bengaluru");
        assertThat(response.state()).isEqualTo("Karnataka");
        assertThat(response.pincode()).isEqualTo("560001");
        assertThat(response.relationshipType()).isEqualTo("PARENT");
        assertThat(response.verified()).isFalse();
        assertThat(response.createdAt()).isNotNull();

        // Verify retrieval via getMyProfile round-trip
        var retrieved = parentProfileService.getMyProfile(ownerPrincipal());
        assertThat(retrieved.id()).isEqualTo(response.id());
        assertThat(retrieved.name()).isEqualTo("Suresh Verma");
    }

    @Test
    @DisplayName("linkStudent_createsLink: persisted link is active and references correct parent and student")
    void linkStudent_createsLink() {
        // First create the parent profile
        var profile = parentProfileService.createProfile(new CreateParentProfileRequest(
                "Priya Verma",
                "+919000000001",
                "priya@test.com",
                null, null, null, null, null, null, "MOTHER", null, null
        ), ownerPrincipal());

        LinkStudentRequest linkRequest = new LinkStudentRequest(
                STUDENT_ID,
                "Arjun Verma",
                CENTER_ID,
                null,
                LocalDate.of(2010, 6, 15),
                "DPS Bengaluru",
                "8th",
                "CBSE",
                "ROLL-101"
        );

        var linkResponse = studentLinkService.linkStudent(profile.id(), linkRequest, ownerPrincipal());

        assertThat(linkResponse).isNotNull();
        assertThat(linkResponse.id()).isNotNull();
        assertThat(linkResponse.parentId()).isEqualTo(profile.id());
        assertThat(linkResponse.studentId()).isEqualTo(STUDENT_ID);
        assertThat(linkResponse.studentName()).isEqualTo("Arjun Verma");
        assertThat(linkResponse.centerId()).isEqualTo(CENTER_ID);
        assertThat(linkResponse.status()).isEqualTo(LinkStatus.ACTIVE);
        assertThat(linkResponse.schoolName()).isEqualTo("DPS Bengaluru");
        assertThat(linkResponse.standard()).isEqualTo("8th");
        assertThat(linkResponse.board()).isEqualTo("CBSE");
        assertThat(linkResponse.rollNumber()).isEqualTo("ROLL-101");

        // Verify it appears in the list of active links
        var links = studentLinkService.listLinkedStudents(profile.id(), ownerPrincipal());
        assertThat(links).hasSize(1);
        assertThat(links.get(0).studentId()).isEqualTo(STUDENT_ID);
    }

    @Test
    @DisplayName("recordFeePayment_persistsWithAmount: payment is stored with correct amount and PENDING status")
    void recordFeePayment_persistsWithAmount() {
        // Create parent profile first
        var profile = parentProfileService.createProfile(new CreateParentProfileRequest(
                "Rajesh Kumar",
                "+919000000002",
                "rajesh@test.com",
                null, null, null, null, null, null, "PARENT", null, null
        ), ownerPrincipal());

        UUID batchId = UUID.randomUUID();
        RecordFeePaymentRequest paymentRequest = new RecordFeePaymentRequest(
                STUDENT_ID,
                CENTER_ID,
                batchId,
                new BigDecimal("15000.00"),
                "INR",
                LocalDate.of(2026, 3, 14),
                "REF-2026-001",
                "March quarterly fee",
                "TUITION",
                "UPI"
        );

        var paymentResponse = feePaymentService.recordPayment(profile.id(), paymentRequest, ownerPrincipal());

        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.id()).isNotNull();
        assertThat(paymentResponse.parentId()).isEqualTo(profile.id());
        assertThat(paymentResponse.studentId()).isEqualTo(STUDENT_ID);
        assertThat(paymentResponse.centerId()).isEqualTo(CENTER_ID);
        assertThat(paymentResponse.batchId()).isEqualTo(batchId);
        assertThat(paymentResponse.amountPaid()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(paymentResponse.currency()).isEqualTo("INR");
        assertThat(paymentResponse.referenceNumber()).isEqualTo("REF-2026-001");
        assertThat(paymentResponse.feeType()).isEqualTo("TUITION");
        assertThat(paymentResponse.paymentMethod()).isEqualTo("UPI");
        assertThat(paymentResponse.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(paymentResponse.paymentDate()).isEqualTo(LocalDate.of(2026, 3, 14));

        // Verify it appears in the list for this parent
        var payments = feePaymentService.listPayments(profile.id(), ownerPrincipal());
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).amountPaid()).isEqualByComparingTo(new BigDecimal("15000.00"));
    }

    @Test
    @DisplayName("linkStudent_defaultCurrency: payment without explicit currency defaults to INR")
    void recordFeePayment_defaultCurrencyIsINR() {
        var profile = parentProfileService.createProfile(new CreateParentProfileRequest(
                "Anita Singh",
                "+919000000003",
                "anita@test.com",
                null, null, null, null, null, null, null, null, null
        ), ownerPrincipal());

        RecordFeePaymentRequest request = new RecordFeePaymentRequest(
                STUDENT_ID,
                CENTER_ID,
                null,
                new BigDecimal("5000.00"),
                null, // omit currency — should default to INR
                LocalDate.now(),
                "REF-NO-CCY",
                null,
                null,
                null
        );

        var response = feePaymentService.recordPayment(profile.id(), request, ownerPrincipal());

        assertThat(response.currency()).isEqualTo("INR");
    }

    @Test
    @DisplayName("createParentProfile_defaultRelationshipType: null relationshipType defaults to PARENT")
    void createParentProfile_defaultRelationshipType() {
        var response = parentProfileService.createProfile(new CreateParentProfileRequest(
                "Default Relation",
                "+919000000004",
                null, null, null, null, null, null, null,
                null, // no relationship type
                null, null
        ), ownerPrincipal());

        assertThat(response.relationshipType()).isEqualTo("PARENT");
    }
}
