// src/test/java/com/edutech/center/CenterOperationsIT.java
package com.edutech.center;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchResponse;
import com.edutech.center.application.dto.CenterLookupResponse;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.CreateBatchRequest;
import com.edutech.center.application.dto.CreateCenterRequest;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.DuplicateCenterCodeException;
import com.edutech.center.application.service.BatchService;
import com.edutech.center.application.service.CenterService;
import com.edutech.center.domain.model.BatchStatus;
import com.edutech.center.domain.model.CenterStatus;
import com.edutech.center.domain.model.Role;
import com.edutech.center.domain.port.out.BatchRepository;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.infrastructure.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for center-svc: center creation, duplicate-code rejection,
 * institution-code lookup, and batch creation/retrieval — backed by a real
 * PostgreSQL container running Flyway migrations.
 *
 * <p>JWT validation and Kafka/Redis are mocked so tests focus solely on
 * the database-facing application and persistence layers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("center-svc — Center Operations Integration Tests")
class CenterOperationsIT {

    // ---------------------------------------------------------------------------
    // Infrastructure: single shared PostgreSQL container (reused across tests)
    // ---------------------------------------------------------------------------

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("center_test")
                    .withUsername("center_user")
                    .withPassword("center_pass");

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // ---------------------------------------------------------------------------
    // Mocked infrastructure beans (Kafka, Redis, JWT)
    // ---------------------------------------------------------------------------

    /** Mocked so Kafka producer does not try to connect to a real broker. */
    @MockBean
    @SuppressWarnings("rawtypes")
    KafkaTemplate kafkaTemplate;

    /**
     * Mocked so the Spring context starts without a real RSA public-key file.
     */
    @MockBean
    JwtTokenValidator jwtTokenValidator;

    // ---------------------------------------------------------------------------
    // Application-layer services under test
    // ---------------------------------------------------------------------------

    @Autowired
    CenterService centerService;

    @Autowired
    BatchService batchService;

    // ---------------------------------------------------------------------------
    // Port-layer repositories (verify persistence side-effects directly)
    // ---------------------------------------------------------------------------

    @Autowired
    CenterRepository centerRepository;

    @Autowired
    BatchRepository batchRepository;

    // ---------------------------------------------------------------------------
    // Shared test identities
    // ---------------------------------------------------------------------------

    static final UUID OWNER_ID = UUID.randomUUID();

    AuthPrincipal superAdminPrincipal;
    AuthPrincipal centerAdminPrincipal;

    @BeforeEach
    void setUpPrincipals() {
        superAdminPrincipal = new AuthPrincipal(OWNER_ID, "admin@test.com",
                Role.SUPER_ADMIN, null, "fp-admin");

        // Kafka mock: return a completed future so event publishing does not block
        when(kafkaTemplate.send(anyString(), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
    }

    // ---------------------------------------------------------------------------
    // Helper: build a valid CreateCenterRequest with a given code
    // ---------------------------------------------------------------------------

    private CreateCenterRequest buildCenterRequest(String code) {
        return new CreateCenterRequest(
                "EduTech Academy " + code,
                code,
                "123 Knowledge Street, Koramangala",
                "Bangalore",
                "Karnataka",
                "560034",
                "9876543210",
                "info." + code.toLowerCase() + "@edutech.com",
                "https://edutech.com/" + code.toLowerCase(),
                null,
                OWNER_ID
        );
    }

    // ---------------------------------------------------------------------------
    // Test 1: createCenter_persistsWithCode
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("createCenter — persists center to center_schema.centers with ACTIVE status and unique code")
    void createCenter_persistsWithCode() {
        CenterResponse response = centerService.createCenter(
                buildCenterRequest("ETBLR01"), superAdminPrincipal);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.code()).isEqualTo("ETBLR01");
        assertThat(response.name()).isEqualTo("EduTech Academy ETBLR01");
        assertThat(response.city()).isEqualTo("Bangalore");
        assertThat(response.state()).isEqualTo("Karnataka");
        assertThat(response.status()).isEqualTo(CenterStatus.ACTIVE);
        assertThat(response.ownerId()).isEqualTo(OWNER_ID);
        assertThat(response.createdAt()).isNotNull();

        // Verify persistence via repository
        Optional<com.edutech.center.domain.model.CoachingCenter> persisted =
                centerRepository.findByCode("ETBLR01");
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getCode()).isEqualTo("ETBLR01");
        assertThat(persisted.get().getStatus()).isEqualTo(CenterStatus.ACTIVE);
    }

    // ---------------------------------------------------------------------------
    // Test 2: createCenter_duplicateCode_throws
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("createCenter — throws DuplicateCenterCodeException when code is already taken")
    void createCenter_duplicateCode_throws() {
        centerService.createCenter(buildCenterRequest("DUPCODE"), superAdminPrincipal);

        assertThatThrownBy(() ->
                centerService.createCenter(buildCenterRequest("DUPCODE"), superAdminPrincipal))
                .isInstanceOf(DuplicateCenterCodeException.class);
    }

    // ---------------------------------------------------------------------------
    // Test 3: createCenter_nonSuperAdmin_throws
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("createCenter — throws CenterAccessDeniedException for non-SUPER_ADMIN caller")
    void createCenter_nonSuperAdmin_throws() {
        AuthPrincipal nonAdmin = new AuthPrincipal(UUID.randomUUID(), "teacher@test.com",
                Role.TEACHER, null, "fp");

        assertThatThrownBy(() ->
                centerService.createCenter(buildCenterRequest("NOADMIN"), nonAdmin))
                .isInstanceOf(CenterAccessDeniedException.class);
    }

    // ---------------------------------------------------------------------------
    // Test 4: lookupByInstitutionCode_returnsCenter
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("lookupByCode — returns CenterLookupResponse (id, name, city) for a known code")
    void lookupByInstitutionCode_returnsCenter() {
        centerService.createCenter(buildCenterRequest("LOOKUP1"), superAdminPrincipal);

        Optional<CenterLookupResponse> result = centerService.lookupByCode("LOOKUP1");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isNotNull();
        assertThat(result.get().name()).isEqualTo("EduTech Academy LOOKUP1");
        assertThat(result.get().city()).isEqualTo("Bangalore");
    }

    // ---------------------------------------------------------------------------
    // Test 5: lookupByCode_unknownCode_returnsEmpty
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("lookupByCode — returns empty Optional for a code that does not exist")
    void lookupByCode_unknownCode_returnsEmpty() {
        Optional<CenterLookupResponse> result = centerService.lookupByCode("NONEXISTENT");

        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------------------
    // Test 6: createBatch_linkedToCenter
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("createBatch — batch is persisted with UPCOMING status linked to the parent center")
    void createBatch_linkedToCenter() {
        // Arrange: create a center first
        CenterResponse center = centerService.createCenter(
                buildCenterRequest("BATCH01"), superAdminPrincipal);

        UUID centerId = center.id();
        // Build a center-admin principal that belongs to this center
        AuthPrincipal ownerPrincipal = new AuthPrincipal(OWNER_ID, "owner@test.com",
                Role.CENTER_ADMIN, centerId, "fp-owner");

        CreateBatchRequest batchRequest = new CreateBatchRequest(
                "Physics Batch 2026",
                "PHY2026A",
                "Physics",
                null, // no teacher assigned yet
                40,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(180)
        );

        BatchResponse response = batchService.createBatch(centerId, batchRequest, ownerPrincipal);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.centerId()).isEqualTo(centerId);
        assertThat(response.name()).isEqualTo("Physics Batch 2026");
        assertThat(response.code()).isEqualTo("PHY2026A");
        assertThat(response.subject()).isEqualTo("Physics");
        assertThat(response.maxStudents()).isEqualTo(40);
        assertThat(response.enrolledCount()).isEqualTo(0);
        assertThat(response.status()).isEqualTo(BatchStatus.UPCOMING);
        assertThat(response.createdAt()).isNotNull();

        // Verify persistence via repository
        List<com.edutech.center.domain.model.Batch> batches =
                batchRepository.findByCenterId(centerId);
        assertThat(batches).hasSize(1);
        assertThat(batches.get(0).getCenterId()).isEqualTo(centerId);
        assertThat(batches.get(0).getStatus()).isEqualTo(BatchStatus.UPCOMING);
    }

    // ---------------------------------------------------------------------------
    // Test 7: createBatch_andActivate_statusTransition
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("updateBatch — batch transitions from UPCOMING to ACTIVE when activated")
    void createBatch_andActivate_statusTransition() {
        CenterResponse center = centerService.createCenter(
                buildCenterRequest("ACTBATCH"), superAdminPrincipal);
        UUID centerId = center.id();

        AuthPrincipal ownerPrincipal = new AuthPrincipal(OWNER_ID, "owner@test.com",
                Role.CENTER_ADMIN, centerId, "fp");

        BatchResponse created = batchService.createBatch(centerId, new CreateBatchRequest(
                "Maths Batch 2026", "MATH2026A", "Mathematics",
                null, 30,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(90)
        ), ownerPrincipal);

        assertThat(created.status()).isEqualTo(BatchStatus.UPCOMING);

        // Activate the batch
        BatchResponse activated = batchService.updateBatch(created.id(),
                new com.edutech.center.application.dto.UpdateBatchRequest(
                        null, // teacherId unchanged
                        BatchStatus.ACTIVE
                ), ownerPrincipal);

        assertThat(activated.status()).isEqualTo(BatchStatus.ACTIVE);

        // Confirm persistence
        Optional<com.edutech.center.domain.model.Batch> fromDb =
                batchRepository.findByIdAndCenterId(created.id(), centerId);
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getStatus()).isEqualTo(BatchStatus.ACTIVE);
    }

    // ---------------------------------------------------------------------------
    // Test 8: listBatches_filterByStatus
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("listBatches — filtering by UPCOMING status returns only UPCOMING batches")
    void listBatches_filterByStatus() {
        CenterResponse center = centerService.createCenter(
                buildCenterRequest("LISTBATCH"), superAdminPrincipal);
        UUID centerId = center.id();

        AuthPrincipal ownerPrincipal = new AuthPrincipal(OWNER_ID, "owner@test.com",
                Role.CENTER_ADMIN, centerId, "fp");

        // Create two batches
        BatchResponse b1 = batchService.createBatch(centerId, new CreateBatchRequest(
                "Chemistry Upcoming", "CHEM01", "Chemistry",
                null, 25, LocalDate.now().plusDays(5), LocalDate.now().plusDays(100)
        ), ownerPrincipal);

        BatchResponse b2 = batchService.createBatch(centerId, new CreateBatchRequest(
                "Biology Active", "BIO01", "Biology",
                null, 25, LocalDate.now().plusDays(1), LocalDate.now().plusDays(100)
        ), ownerPrincipal);

        // Activate b2
        batchService.updateBatch(b2.id(), new com.edutech.center.application.dto.UpdateBatchRequest(
                null, // teacherId unchanged
                BatchStatus.ACTIVE
        ), ownerPrincipal);

        // List only UPCOMING batches
        List<BatchResponse> upcoming = batchService.listBatches(centerId, BatchStatus.UPCOMING, ownerPrincipal);

        assertThat(upcoming)
                .isNotEmpty()
                .allSatisfy(b -> assertThat(b.status()).isEqualTo(BatchStatus.UPCOMING));
        assertThat(upcoming).extracting(BatchResponse::code).contains("CHEM01");
        assertThat(upcoming).extracting(BatchResponse::code).doesNotContain("BIO01");
    }
}
