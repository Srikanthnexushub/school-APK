// src/test/java/com/edutech/center/CenterControllerIT.java
package com.edutech.center;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchResponse;
import com.edutech.center.application.dto.CenterLookupResponse;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.CreateBatchRequest;
import com.edutech.center.application.dto.CreateCenterRequest;
import com.edutech.center.application.dto.UpdateBatchRequest;
import com.edutech.center.application.dto.UpdateCenterRequest;
import com.edutech.center.domain.model.BatchStatus;
import com.edutech.center.domain.model.CenterStatus;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HTTP-level integration tests for center-svc controllers.
 *
 * <p>Uses native local Postgres (env vars: POSTGRES_HOST/PORT/CENTER_SVC_DB_NAME/USERNAME/PASSWORD)
 * with Flyway migrations applied automatically. Kafka and the JWT validator are mocked — the JWT
 * validator returns a pre-configured {@link AuthPrincipal} for any bearer token
 * so that Spring Security does not block requests.
 *
 * <p>Test coverage:
 * <ul>
 *   <li>POST   /api/v1/centers          — creates center as SUPER_ADMIN, returns 201</li>
 *   <li>GET    /api/v1/centers/{id}     — retrieves center by id, returns 200</li>
 *   <li>GET    /api/v1/centers/lookup?code=X — public lookup, returns 200/404</li>
 *   <li>PUT    /api/v1/centers/{id}     — updates center details, returns 200</li>
 *   <li>POST   /api/v1/centers/{id}/batches — creates batch inside center, returns 201</li>
 *   <li>PUT    /api/v1/centers/{centerId}/batches/{batchId} — activates batch</li>
 *   <li>POST   /api/v1/centers with non-SUPER_ADMIN — returns 403</li>
 *   <li>POST   /api/v1/centers with duplicate code — returns 409</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("center-svc — Controller HTTP Integration Tests")
class CenterControllerIT {

    // -------------------------------------------------------------------------
    // Mocked infrastructure beans
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Test collaborators
    // -------------------------------------------------------------------------

    @Autowired
    TestRestTemplate restTemplate;

    // -------------------------------------------------------------------------
    // Shared test identities
    // -------------------------------------------------------------------------

    /** Per-run random suffix — prevents unique-constraint conflicts with previous test runs. */
    private static final String R = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();

    static final UUID OWNER_ID = UUID.randomUUID();

    static final String FAKE_TOKEN = "test-token";

    AuthPrincipal superAdminPrincipal;
    AuthPrincipal centerAdminPrincipal;

    @BeforeEach
    void setUp() {
        superAdminPrincipal = new AuthPrincipal(OWNER_ID, "admin@test.com",
                Role.SUPER_ADMIN, null, "fp-admin");
        // centerId will be filled per-test when needed
        centerAdminPrincipal = new AuthPrincipal(OWNER_ID, "owner@test.com",
                Role.CENTER_ADMIN, null, "fp-owner");

        // Default auth: super-admin
        mockAuth(superAdminPrincipal);


    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void mockAuth(AuthPrincipal principal) {
        when(jwtTokenValidator.validate(anyString())).thenReturn(Optional.of(principal));
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(FAKE_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private <T> HttpEntity<T> authEntity(T body) {
        return new HttpEntity<>(body, authHeaders());
    }

    private HttpEntity<Void> authEntity() {
        return new HttpEntity<>(authHeaders());
    }

    /** Builds a valid CreateCenterRequest with a given unique code. */
    private CreateCenterRequest buildCenterRequest(String code) {
        return new CreateCenterRequest(
                "EduTech Academy " + code,
                code,
                "456 Learning Ave, Indiranagar",
                "Bangalore",
                "Karnataka",
                "560038",
                "9876500000",
                "info." + code.toLowerCase() + "@center.com",
                "https://center.com/" + code.toLowerCase(),
                null,
                OWNER_ID,
                null
        );
    }

    /** Builds a valid CreateBatchRequest. */
    private CreateBatchRequest buildBatchRequest(String name, String code) {
        return new CreateBatchRequest(
                name,
                code,
                "Mathematics",
                null,    // no teacher yet
                35,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(180),
                com.edutech.center.domain.model.BatchMode.OFFLINE
        );
    }

    // =========================================================================
    // Test 1: POST /api/v1/centers — creates center, returns 201
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/centers — returns 201 Created with ACTIVE center body")
    void createCenter_returns201WithActiveStatus() {
        String code = "C01" + R;
        CreateCenterRequest request = buildCenterRequest(code);

        ResponseEntity<CenterResponse> response = restTemplate.exchange(
                "/api/v1/centers",
                HttpMethod.POST,
                authEntity(request),
                CenterResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        CenterResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.code()).isEqualTo(code);
        assertThat(body.name()).isEqualTo("EduTech Academy " + code);
        assertThat(body.city()).isEqualTo("Bangalore");
        assertThat(body.state()).isEqualTo("Karnataka");
        assertThat(body.status()).isEqualTo(CenterStatus.ACTIVE);
        assertThat(body.ownerId()).isEqualTo(OWNER_ID);
        assertThat(body.createdAt()).isNotNull();
    }

    // =========================================================================
    // Test 2: GET /api/v1/centers/{centerId} — retrieves center by ID
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/centers/{centerId} — returns 200 with center details")
    void getCenter_returns200WithCenterDetails() {
        // Create center first
        String code = "C02" + R;
        CenterResponse created = restTemplate.exchange(
                "/api/v1/centers",
                HttpMethod.POST,
                authEntity(buildCenterRequest(code)),
                CenterResponse.class).getBody();
        UUID centerId = created.id();

        // Retrieve it
        ResponseEntity<CenterResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId,
                HttpMethod.GET,
                authEntity(),
                CenterResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CenterResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(centerId);
        assertThat(body.code()).isEqualTo(code);
        assertThat(body.name()).isEqualTo("EduTech Academy " + code);
    }

    // =========================================================================
    // Test 3: GET /api/v1/centers/lookup?code=X — public lookup, no auth required
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/centers/lookup?code=LOOKUP01 — returns 200 with lookup data (public endpoint)")
    void lookupCenter_returns200ForKnownCode() {
        // Create the center first (as admin)
        String code = "C03" + R;
        restTemplate.exchange(
                "/api/v1/centers",
                HttpMethod.POST,
                authEntity(buildCenterRequest(code)),
                CenterResponse.class);

        // Lookup without any Authorization header (SecurityConfig permits /api/v1/centers/lookup)
        ResponseEntity<CenterLookupResponse> response = restTemplate.exchange(
                "/api/v1/centers/lookup?code=" + code,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                CenterLookupResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CenterLookupResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.name()).isEqualTo("EduTech Academy " + code);
        assertThat(body.city()).isEqualTo("Bangalore");
    }

    @Test
    @DisplayName("GET /api/v1/centers/lookup?code=UNKNOWN — returns 404 for unknown code")
    void lookupCenter_returns404ForUnknownCode() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/centers/lookup?code=DOESNOTEXIST",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // =========================================================================
    // Test 5: PUT /api/v1/centers/{centerId} — updates center details
    // =========================================================================

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId} — returns 200 with updated center name and city")
    void updateCenter_returns200WithUpdatedFields() {
        // Create
        CenterResponse created = restTemplate.exchange(
                "/api/v1/centers",
                HttpMethod.POST,
                authEntity(buildCenterRequest("C04" + R)),
                CenterResponse.class).getBody();
        UUID centerId = created.id();

        // Update
        UpdateCenterRequest updateRequest = new UpdateCenterRequest(
                "Updated Academy UPDTEST",
                "789 New Street, MG Road",
                "Hyderabad",
                "Telangana",
                "500001",
                "9988776655",
                "updated@center.com",
                null,
                null,
                null
        );

        ResponseEntity<CenterResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId,
                HttpMethod.PUT,
                authEntity(updateRequest),
                CenterResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CenterResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.name()).isEqualTo("Updated Academy UPDTEST");
        assertThat(body.city()).isEqualTo("Hyderabad");
        assertThat(body.state()).isEqualTo("Telangana");
        assertThat(body.id()).isEqualTo(centerId);
    }

    // =========================================================================
    // Test 6: POST /api/v1/centers/{centerId}/batches — creates batch
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/batches — returns 201 with UPCOMING batch")
    void createBatch_returns201WithUpcomingStatus() {
        // Create parent center
        CenterResponse center = restTemplate.exchange(
                "/api/v1/centers",
                HttpMethod.POST,
                authEntity(buildCenterRequest("C05" + R)),
                CenterResponse.class).getBody();
        UUID centerId = center.id();

        // Switch to CENTER_ADMIN principal that belongs to this center
        AuthPrincipal ownerPrincipal = new AuthPrincipal(OWNER_ID, "owner@test.com",
                Role.CENTER_ADMIN, centerId, "fp-owner");
        mockAuth(ownerPrincipal);

        String batchCode = "PH6" + R;
        ResponseEntity<BatchResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches",
                HttpMethod.POST,
                authEntity(buildBatchRequest("Physics Batch 2026", batchCode)),
                BatchResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BatchResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.centerId()).isEqualTo(centerId);
        assertThat(body.name()).isEqualTo("Physics Batch 2026");
        assertThat(body.code()).isEqualTo(batchCode);
        assertThat(body.subject()).isEqualTo("Mathematics");
        assertThat(body.maxStudents()).isEqualTo(35);
        assertThat(body.enrolledCount()).isEqualTo(0);
        assertThat(body.status()).isEqualTo(BatchStatus.UPCOMING);
        assertThat(body.createdAt()).isNotNull();
    }

    // =========================================================================
    // Test 7: PUT /api/v1/centers/{centerId}/batches/{batchId} — activates batch
    // =========================================================================

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId}/batches/{batchId} — batch transitions UPCOMING → ACTIVE")
    void updateBatch_activates_returns200WithActiveStatus() {
        // Create center
        CenterResponse center = restTemplate.exchange(
                "/api/v1/centers",
                HttpMethod.POST,
                authEntity(buildCenterRequest("C06" + R)),
                CenterResponse.class).getBody();
        UUID centerId = center.id();

        AuthPrincipal ownerPrincipal = new AuthPrincipal(OWNER_ID, "owner@test.com",
                Role.CENTER_ADMIN, centerId, "fp-owner");
        mockAuth(ownerPrincipal);

        // Create batch
        BatchResponse batch = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches",
                HttpMethod.POST,
                authEntity(buildBatchRequest("Chemistry Batch 2026", "CH7" + R)),
                BatchResponse.class).getBody();
        UUID batchId = batch.id();
        assertThat(batch.status()).isEqualTo(BatchStatus.UPCOMING);

        // Activate
        UpdateBatchRequest activateRequest = new UpdateBatchRequest(null, BatchStatus.ACTIVE);
        ResponseEntity<BatchResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches/" + batchId,
                HttpMethod.PUT,
                authEntity(activateRequest),
                BatchResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(BatchStatus.ACTIVE);
        assertThat(response.getBody().id()).isEqualTo(batchId);
    }

    // =========================================================================
    // Test 8: POST /api/v1/centers with non-SUPER_ADMIN → 403
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/centers as TEACHER — returns 403 Forbidden")
    void createCenter_asTeacher_returns403() {
        AuthPrincipal teacherPrincipal = new AuthPrincipal(UUID.randomUUID(), "teacher@test.com",
                Role.TEACHER, null, "fp-teacher");
        mockAuth(teacherPrincipal);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/centers",
                HttpMethod.POST,
                authEntity(buildCenterRequest("C07" + R)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // Test 9: POST /api/v1/centers with duplicate code → 409
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/centers with duplicate code — returns 409 Conflict")
    void createCenter_duplicateCode_returns409() {
        // Create first center with a unique-per-run duplicate code
        String dupCode = "C08" + R;
        restTemplate.exchange(
                "/api/v1/centers",
                HttpMethod.POST,
                authEntity(buildCenterRequest(dupCode)),
                CenterResponse.class);

        // Attempt to create second center with same code
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/centers",
                HttpMethod.POST,
                authEntity(buildCenterRequest(dupCode)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // =========================================================================
    // REGRESSION — Fix #349: STUDENT and PARENT must not receive 403 on
    // GET /api/v1/centers/{centerId}.  Prior to the fix, CenterService.getCenter()
    // only checked belongsToCenter() which requires a matching centerId in the JWT;
    // students registered before the centerId-backfill had null → always 403.
    // =========================================================================

    @Test
    @DisplayName("REGRESSION Fix#349 — GET /api/v1/centers/{id} as STUDENT returns 200 (not 403)")
    void getCenter_asStudent_returns200() {
        // Create center as super-admin
        String code = "C10" + R;
        CenterResponse created = restTemplate.exchange(
                "/api/v1/centers",
                HttpMethod.POST,
                authEntity(buildCenterRequest(code)),
                CenterResponse.class).getBody();
        UUID centerId = created.id();

        // Switch to STUDENT — centerId intentionally null to simulate pre-backfill JWT
        AuthPrincipal studentPrincipal = new AuthPrincipal(UUID.randomUUID(), "student@test.com",
                Role.STUDENT, null, "fp-student");
        mockAuth(studentPrincipal);

        ResponseEntity<CenterResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId,
                HttpMethod.GET,
                authEntity(),
                CenterResponse.class);

        assertThat(response.getStatusCode())
                .as("Student must be allowed to read center details (Fix #349)")
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(centerId);
    }

    @Test
    @DisplayName("REGRESSION Fix#349 — GET /api/v1/centers/{id} as PARENT returns 200 (not 403)")
    void getCenter_asParent_returns200() {
        // Create center as super-admin
        String code = "C11" + R;
        CenterResponse created = restTemplate.exchange(
                "/api/v1/centers",
                HttpMethod.POST,
                authEntity(buildCenterRequest(code)),
                CenterResponse.class).getBody();
        UUID centerId = created.id();

        // Switch to PARENT with null centerId
        AuthPrincipal parentPrincipal = new AuthPrincipal(UUID.randomUUID(), "parent@test.com",
                Role.PARENT, null, "fp-parent");
        mockAuth(parentPrincipal);

        ResponseEntity<CenterResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId,
                HttpMethod.GET,
                authEntity(),
                CenterResponse.class);

        assertThat(response.getStatusCode())
                .as("Parent must be allowed to read center details (Fix #349)")
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(centerId);
    }
}
