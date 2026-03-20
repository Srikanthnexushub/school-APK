// src/test/java/com/edutech/center/JobPostingControllerIT.java
package com.edutech.center;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.CreateCenterRequest;
import com.edutech.center.application.dto.CreateJobPostingRequest;
import com.edutech.center.application.dto.JobPostingResponse;
import com.edutech.center.application.dto.StatusUpdateRequest;
import com.edutech.center.application.dto.UpdateJobPostingRequest;
import com.edutech.center.domain.model.JobPostingStatus;
import com.edutech.center.domain.model.JobType;
import com.edutech.center.domain.model.Role;
import com.edutech.center.domain.model.StaffRoleType;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HTTP-level integration tests for {@link com.edutech.center.api.JobPostingController}.
 *
 * <p>Uses a real PostgreSQL container via TestContainers with Flyway migrations
 * applied automatically. Kafka and the JWT validator are mocked — the JWT
 * validator returns a pre-configured {@link AuthPrincipal} for any bearer token
 * so that Spring Security does not block requests.
 *
 * <p>Test coverage:
 * <ul>
 *   <li>POST   /api/v1/centers/{centerId}/jobs           — create OPEN job, returns 201</li>
 *   <li>POST   /api/v1/centers/{centerId}/jobs           — create DRAFT job, returns 201</li>
 *   <li>POST   /api/v1/centers/{centerId}/jobs           — no auth header, returns 403</li>
 *   <li>POST   /api/v1/centers/{centerId}/jobs           — wrong center admin, returns 403</li>
 *   <li>GET    /api/v1/centers/{centerId}/jobs           — list own jobs, returns 200 with jobs</li>
 *   <li>GET    /api/v1/centers/{centerId}/jobs/{jobId}   — get by id, returns 200</li>
 *   <li>GET    /api/v1/centers/{centerId}/jobs/{jobId}   — unknown id, returns 404</li>
 *   <li>PUT    /api/v1/centers/{centerId}/jobs/{jobId}   — update fields, returns 200</li>
 *   <li>PATCH  /api/v1/centers/{centerId}/jobs/{jobId}/status — DRAFT→OPEN, returns 200</li>
 *   <li>PATCH  /api/v1/centers/{centerId}/jobs/{jobId}/status — OPEN→CLOSED, returns 200</li>
 *   <li>PATCH  /api/v1/centers/{centerId}/jobs/{jobId}/status — OPEN→FILLED, returns 200</li>
 *   <li>PATCH  /api/v1/centers/{centerId}/jobs/{jobId}/status — DRAFT→FILLED (invalid), returns 422</li>
 *   <li>DELETE /api/v1/centers/{centerId}/jobs/{jobId}   — soft-delete, returns 204 then 404</li>
 *   <li>GET    /api/v1/jobs                              — board shows only OPEN jobs</li>
 *   <li>GET    /api/v1/jobs?roleType=TEACHER             — filter by role type</li>
 *   <li>GET    /api/v1/jobs?city=Bengaluru               — filter by city</li>
 *   <li>GET    /api/v1/jobs                              — paginated Spring Page structure</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("center-svc — JobPosting Controller HTTP Integration Tests")
class JobPostingControllerIT {

    // -------------------------------------------------------------------------
    // Infrastructure: single shared PostgreSQL container
    // -------------------------------------------------------------------------

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("center_jobs_test")
                    .withUsername("jobs_user")
                    .withPassword("jobs_pass");

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // -------------------------------------------------------------------------
    // Mocked infrastructure beans
    // -------------------------------------------------------------------------

    @MockBean
    @SuppressWarnings("rawtypes")
    KafkaTemplate kafkaTemplate;

    @MockBean
    JwtTokenValidator jwtTokenValidator;

    // -------------------------------------------------------------------------
    // Test collaborators
    // -------------------------------------------------------------------------

    @Autowired
    TestRestTemplate restTemplate;

    // -------------------------------------------------------------------------
    // Shared test identities
    // -------------------------------------------------------------------------

    static final UUID ADMIN_USER_ID = UUID.randomUUID();
    static final UUID OTHER_USER_ID = UUID.randomUUID();

    static final String FAKE_TOKEN = "test-token";

    AuthPrincipal superAdminPrincipal;
    AuthPrincipal centerAdminPrincipal;

    @BeforeEach
    void setUp() {
        superAdminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "superadmin@test.com",
                Role.SUPER_ADMIN, null, "fp-superadmin");
        // centerId is set to null initially; tests that need center-scoped access
        // will create a center first and then build a principal with the actual centerId.
        centerAdminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, null, "fp-admin");

        // Default auth: super-admin (can create centers)
        mockAuth(superAdminPrincipal);

        // Kafka mock — suppress all publish calls
        when(kafkaTemplate.send(anyString(), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
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

    /**
     * Creates a center as SUPER_ADMIN and returns its UUID.
     * Always uses a unique code derived from a random UUID suffix to avoid conflicts
     * between tests that share the same container across the test class run.
     */
    private UUID createCenter() {
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        mockAuth(superAdminPrincipal);
        CreateCenterRequest req = new CreateCenterRequest(
                "Test Academy " + uniqueSuffix,
                "CTR" + uniqueSuffix,
                "123 Main Street, Indiranagar",
                "Bengaluru",
                "Karnataka",
                "560038",
                "9876500000",
                "info." + uniqueSuffix.toLowerCase() + "@academy.com",
                null,
                null,
                ADMIN_USER_ID
        );
        ResponseEntity<CenterResponse> resp = restTemplate.exchange(
                "/api/v1/centers",
                HttpMethod.POST,
                authEntity(req),
                CenterResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    /**
     * Creates a job posting for the given center and returns its UUID.
     * The caller is responsible for setting the correct mock auth principal before calling.
     */
    private UUID createJob(UUID centerId, String title, StaffRoleType roleType,
                           JobType jobType, JobPostingStatus status) {
        CreateJobPostingRequest req = new CreateJobPostingRequest(
                title,
                "We are looking for a passionate " + roleType.getDisplayName(),
                roleType,
                roleType == StaffRoleType.TEACHER ? "Mathematics,Physics" : null,
                "B.Ed, M.Sc",
                2,
                jobType,
                30000,
                60000,
                LocalDate.now().plusMonths(2),
                status
        );
        ResponseEntity<JobPostingResponse> resp = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs",
                HttpMethod.POST,
                authEntity(req),
                JobPostingResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    // =========================================================================
    // Test 1: POST /api/v1/centers/{centerId}/jobs — creates OPEN job, returns 201
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{centerId}/jobs — CENTER_ADMIN creates FULL_TIME TEACHER job → 201, status=OPEN")
    void createJob_asAdmin_returns201() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        CreateJobPostingRequest req = new CreateJobPostingRequest(
                "Senior Mathematics Teacher",
                "Seeking an experienced Mathematics teacher for JEE preparation.",
                StaffRoleType.TEACHER,
                "Mathematics,Physics",
                "B.Ed, M.Sc Mathematics",
                3,
                JobType.FULL_TIME,
                40000,
                70000,
                LocalDate.now().plusMonths(3),
                null  // null → defaults to OPEN
        );

        ResponseEntity<JobPostingResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs",
                HttpMethod.POST,
                authEntity(req),
                JobPostingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JobPostingResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.centerId()).isEqualTo(centerId);
        assertThat(body.title()).isEqualTo("Senior Mathematics Teacher");
        assertThat(body.roleType()).isEqualTo(StaffRoleType.TEACHER);
        assertThat(body.jobType()).isEqualTo(JobType.FULL_TIME);
        assertThat(body.status()).isEqualTo(JobPostingStatus.OPEN);
        assertThat(body.salaryMin()).isEqualTo(40000);
        assertThat(body.salaryMax()).isEqualTo(70000);
        assertThat(body.postedAt()).isNotNull();
    }

    // =========================================================================
    // Test 2: POST with status=DRAFT — returns 201 with status=DRAFT
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{centerId}/jobs with status=DRAFT — returns 201, status=DRAFT")
    void createJob_asDraftStatus_returns201WithDraftStatus() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        CreateJobPostingRequest req = new CreateJobPostingRequest(
                "Draft Counselor Role",
                "A draft posting for a student counselor.",
                StaffRoleType.COUNSELOR,
                null,
                "M.A. Psychology",
                1,
                JobType.FULL_TIME,
                25000,
                45000,
                LocalDate.now().plusMonths(1),
                JobPostingStatus.DRAFT
        );

        ResponseEntity<JobPostingResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs",
                HttpMethod.POST,
                authEntity(req),
                JobPostingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JobPostingResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.status()).isEqualTo(JobPostingStatus.DRAFT);
        assertThat(body.roleType()).isEqualTo(StaffRoleType.COUNSELOR);
    }

    // =========================================================================
    // Test 3: POST without Authorization header — returns 403
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{centerId}/jobs without auth header — returns 403 Forbidden")
    void createJob_withoutAuth_returns403() {
        UUID centerId = createCenter();

        CreateJobPostingRequest req = new CreateJobPostingRequest(
                "Unauthenticated Job",
                "This should not be created.",
                StaffRoleType.TEACHER,
                "Physics",
                null,
                0,
                JobType.CONTRACT,
                null,
                null,
                null,
                null
        );

        // No auth header
        HttpHeaders noAuthHeaders = new HttpHeaders();
        noAuthHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateJobPostingRequest> entity = new HttpEntity<>(req, noAuthHeaders);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs",
                HttpMethod.POST,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // Test 4: POST by CENTER_ADMIN for a different center — returns 403
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{centerId}/jobs by wrong CENTER_ADMIN — returns 403 Forbidden")
    void createJob_byWrongCenter_returns403() {
        UUID centerA = createCenter();
        UUID centerB = createCenter();

        // Admin for centerA tries to post a job to centerB
        AuthPrincipal wrongAdmin = new AuthPrincipal(OTHER_USER_ID, "wrong@admin.com",
                Role.CENTER_ADMIN, centerA, "fp-wrong");
        mockAuth(wrongAdmin);

        CreateJobPostingRequest req = new CreateJobPostingRequest(
                "Unauthorized Job",
                "Admin from wrong center.",
                StaffRoleType.TEACHER,
                "Chemistry",
                null,
                0,
                JobType.FULL_TIME,
                null,
                null,
                null,
                null
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/centers/" + centerB + "/jobs",
                HttpMethod.POST,
                authEntity(req),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // Test 5: GET /api/v1/centers/{centerId}/jobs — list returns all created jobs
    // =========================================================================

    @Test
    @DisplayName("GET /centers/{centerId}/jobs — returns 200 with all center jobs listed")
    void listOwnJobs_returns200WithJobs() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        // Create two jobs
        createJob(centerId, "Physics Teacher", StaffRoleType.TEACHER, JobType.FULL_TIME, JobPostingStatus.OPEN);
        createJob(centerId, "Lab Assistant Wanted", StaffRoleType.LAB_ASSISTANT, JobType.PART_TIME, JobPostingStatus.DRAFT);

        ResponseEntity<List<JobPostingResponse>> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<JobPostingResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<JobPostingResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).hasSizeGreaterThanOrEqualTo(2);
        assertThat(body).extracting(JobPostingResponse::title)
                .contains("Physics Teacher", "Lab Assistant Wanted");
    }

    // =========================================================================
    // Test 6: GET /api/v1/centers/{centerId}/jobs/{jobId} — returns 200 with fields
    // =========================================================================

    @Test
    @DisplayName("GET /centers/{centerId}/jobs/{jobId} — returns 200 with correct job fields")
    void getJob_returns200() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        UUID jobId = createJob(centerId, "HOD Chemistry", StaffRoleType.HOD, JobType.FULL_TIME, JobPostingStatus.OPEN);

        ResponseEntity<JobPostingResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs/" + jobId,
                HttpMethod.GET,
                authEntity(),
                JobPostingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JobPostingResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(jobId);
        assertThat(body.centerId()).isEqualTo(centerId);
        assertThat(body.title()).isEqualTo("HOD Chemistry");
        assertThat(body.roleType()).isEqualTo(StaffRoleType.HOD);
        assertThat(body.jobType()).isEqualTo(JobType.FULL_TIME);
        assertThat(body.status()).isEqualTo(JobPostingStatus.OPEN);
    }

    // =========================================================================
    // Test 7: GET with unknown jobId — returns 404
    // =========================================================================

    @Test
    @DisplayName("GET /centers/{centerId}/jobs/{randomId} — returns 404 Not Found")
    void getJob_notFound_returns404() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        UUID nonExistentJobId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs/" + nonExistentJobId,
                HttpMethod.GET,
                authEntity(),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // =========================================================================
    // Test 8: PUT /api/v1/centers/{centerId}/jobs/{jobId} — update fields
    // =========================================================================

    @Test
    @DisplayName("PUT /centers/{centerId}/jobs/{jobId} — returns 200 with updated title and description")
    void updateJob_returns200WithUpdatedFields() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        UUID jobId = createJob(centerId, "Original Title", StaffRoleType.TEACHER, JobType.FULL_TIME, JobPostingStatus.OPEN);

        UpdateJobPostingRequest updateReq = new UpdateJobPostingRequest(
                "Updated Senior Teacher Title",
                "Updated description for the teaching role.",
                null,   // roleType unchanged
                null,   // subjects unchanged
                "B.Ed, M.Sc, PhD preferred",
                5,
                null,   // jobType unchanged
                50000,
                80000,
                LocalDate.now().plusMonths(4)
        );

        ResponseEntity<JobPostingResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs/" + jobId,
                HttpMethod.PUT,
                authEntity(updateReq),
                JobPostingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JobPostingResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(jobId);
        assertThat(body.title()).isEqualTo("Updated Senior Teacher Title");
        assertThat(body.description()).isEqualTo("Updated description for the teaching role.");
        assertThat(body.experienceMinYears()).isEqualTo(5);
        assertThat(body.salaryMin()).isEqualTo(50000);
        assertThat(body.salaryMax()).isEqualTo(80000);
        // Role type should remain TEACHER (was not updated)
        assertThat(body.roleType()).isEqualTo(StaffRoleType.TEACHER);
    }

    // =========================================================================
    // Test 9: PATCH status DRAFT → OPEN — returns 200, status=OPEN
    // =========================================================================

    @Test
    @DisplayName("PATCH /centers/{centerId}/jobs/{jobId}/status — DRAFT → OPEN returns 200")
    void updateStatus_draftToOpen_returns200() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        UUID jobId = createJob(centerId, "Draft Job To Publish", StaffRoleType.COORDINATOR,
                JobType.FULL_TIME, JobPostingStatus.DRAFT);

        StatusUpdateRequest statusReq = new StatusUpdateRequest(JobPostingStatus.OPEN);

        ResponseEntity<JobPostingResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs/" + jobId + "/status",
                HttpMethod.PATCH,
                authEntity(statusReq),
                JobPostingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JobPostingResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(jobId);
        assertThat(body.status()).isEqualTo(JobPostingStatus.OPEN);
    }

    // =========================================================================
    // Test 10: PATCH status OPEN → CLOSED — returns 200, status=CLOSED
    // =========================================================================

    @Test
    @DisplayName("PATCH /centers/{centerId}/jobs/{jobId}/status — OPEN → CLOSED returns 200")
    void updateStatus_openToClosed_returns200() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        UUID jobId = createJob(centerId, "Open Job To Close", StaffRoleType.LIBRARIAN,
                JobType.PART_TIME, JobPostingStatus.OPEN);

        StatusUpdateRequest statusReq = new StatusUpdateRequest(JobPostingStatus.CLOSED);

        ResponseEntity<JobPostingResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs/" + jobId + "/status",
                HttpMethod.PATCH,
                authEntity(statusReq),
                JobPostingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JobPostingResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(JobPostingStatus.CLOSED);
    }

    // =========================================================================
    // Test 11: PATCH status OPEN → FILLED — returns 200, status=FILLED
    // =========================================================================

    @Test
    @DisplayName("PATCH /centers/{centerId}/jobs/{jobId}/status — OPEN → FILLED returns 200")
    void updateStatus_openToFilled_returns200() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        UUID jobId = createJob(centerId, "Filled Sports Coach Role", StaffRoleType.SPORTS_COACH,
                JobType.CONTRACT, JobPostingStatus.OPEN);

        StatusUpdateRequest statusReq = new StatusUpdateRequest(JobPostingStatus.FILLED);

        ResponseEntity<JobPostingResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs/" + jobId + "/status",
                HttpMethod.PATCH,
                authEntity(statusReq),
                JobPostingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JobPostingResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(JobPostingStatus.FILLED);
    }

    // =========================================================================
    // Test 12: PATCH status DRAFT → FILLED (invalid transition) — returns 422
    // =========================================================================

    @Test
    @DisplayName("PATCH /centers/{centerId}/jobs/{jobId}/status — DRAFT → FILLED (invalid) returns 422")
    void updateStatus_invalidTransition_returns422() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        // Create a DRAFT posting — FILLED requires OPEN first (DRAFT→OPEN→FILLED)
        UUID jobId = createJob(centerId, "Invalid Transition Job", StaffRoleType.ADMIN_STAFF,
                JobType.FULL_TIME, JobPostingStatus.DRAFT);

        StatusUpdateRequest statusReq = new StatusUpdateRequest(JobPostingStatus.FILLED);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs/" + jobId + "/status",
                HttpMethod.PATCH,
                authEntity(statusReq),
                String.class);

        // GlobalExceptionHandler maps IllegalStateException → 422 UNPROCESSABLE_ENTITY
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // =========================================================================
    // Test 13: DELETE — soft-delete returns 204, subsequent GET returns 404
    // =========================================================================

    @Test
    @DisplayName("DELETE /centers/{centerId}/jobs/{jobId} — returns 204, GET afterwards returns 404")
    void deleteJob_returns204() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        UUID jobId = createJob(centerId, "Job To Be Deleted", StaffRoleType.TEACHER,
                JobType.FULL_TIME, JobPostingStatus.OPEN);

        // Soft-delete
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs/" + jobId,
                HttpMethod.DELETE,
                authEntity(),
                Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify it is no longer accessible
        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/jobs/" + jobId,
                HttpMethod.GET,
                authEntity(),
                String.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // =========================================================================
    // Test 14: GET /api/v1/jobs — board returns only OPEN jobs
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/jobs — public board returns only OPEN jobs, not DRAFT")
    void listJobBoard_returnsOnlyOpenJobs() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        String openTitle  = "Open Board Job " + UUID.randomUUID();
        String draftTitle = "Draft Board Job " + UUID.randomUUID();

        createJob(centerId, openTitle,  StaffRoleType.TEACHER, JobType.FULL_TIME, JobPostingStatus.OPEN);
        createJob(centerId, draftTitle, StaffRoleType.TEACHER, JobType.FULL_TIME, JobPostingStatus.DRAFT);

        // Any authenticated user may access the board
        mockAuth(superAdminPrincipal);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/jobs",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> page = response.getBody();
        assertThat(page).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).isNotNull();

        List<String> titles = content.stream()
                .map(j -> (String) j.get("title"))
                .toList();

        assertThat(titles).contains(openTitle);
        assertThat(titles).doesNotContain(draftTitle);
    }

    // =========================================================================
    // Test 15: GET /api/v1/jobs?roleType=TEACHER — filter by role type
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/jobs?roleType=TEACHER — returns only TEACHER jobs, not COUNSELOR")
    void listJobBoard_filterByRoleType() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        String teacherTitle   = "Teacher Filter Job " + UUID.randomUUID();
        String counselorTitle = "Counselor Filter Job " + UUID.randomUUID();

        createJob(centerId, teacherTitle,   StaffRoleType.TEACHER,   JobType.FULL_TIME, JobPostingStatus.OPEN);
        createJob(centerId, counselorTitle, StaffRoleType.COUNSELOR, JobType.FULL_TIME, JobPostingStatus.OPEN);

        mockAuth(superAdminPrincipal);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/jobs?roleType=TEACHER",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> page = response.getBody();
        assertThat(page).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).isNotNull();

        List<String> titles = content.stream()
                .map(j -> (String) j.get("title"))
                .toList();

        assertThat(titles).contains(teacherTitle);
        assertThat(titles).doesNotContain(counselorTitle);
    }

    // =========================================================================
    // Test 16: GET /api/v1/jobs?city=Bengaluru — filter by city
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/jobs?city=Bengaluru — returns jobs whose center city contains 'Bengaluru'")
    void listJobBoard_filterByCity_returnsMatching() {
        // The center created by createCenter() is in "Bengaluru"
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        String cityJobTitle = "Bengaluru City Job " + UUID.randomUUID();
        createJob(centerId, cityJobTitle, StaffRoleType.HOD, JobType.FULL_TIME, JobPostingStatus.OPEN);

        mockAuth(superAdminPrincipal);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/jobs?city=Bengaluru",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> page = response.getBody();
        assertThat(page).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).isNotNull();

        List<String> titles = content.stream()
                .map(j -> (String) j.get("title"))
                .toList();

        assertThat(titles).contains(cityJobTitle);
    }

    // =========================================================================
    // Test 17: GET /api/v1/jobs — response has Spring Page structure
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/jobs — response is a Spring Page with content, totalElements, number, size")
    void listJobBoard_noFilters_isPaginated() {
        UUID centerId = createCenter();
        AuthPrincipal adminPrincipal = new AuthPrincipal(ADMIN_USER_ID, "admin@center.com",
                Role.CENTER_ADMIN, centerId, "fp-admin");
        mockAuth(adminPrincipal);

        createJob(centerId, "Paginated Job " + UUID.randomUUID(),
                StaffRoleType.TEACHER, JobType.FULL_TIME, JobPostingStatus.OPEN);

        mockAuth(superAdminPrincipal);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/jobs",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> page = response.getBody();
        assertThat(page).isNotNull();

        // Verify standard Spring Page structure keys are present
        assertThat(page).containsKey("content");
        assertThat(page).containsKey("totalElements");
        assertThat(page).containsKey("number");
        assertThat(page).containsKey("size");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).isNotNull();
        assertThat(content).isNotEmpty();

        // totalElements must be >= 1 (we just created one OPEN job)
        Number totalElements = (Number) page.get("totalElements");
        assertThat(totalElements.longValue()).isGreaterThanOrEqualTo(1L);

        // Page number defaults to 0 (first page)
        Number number = (Number) page.get("number");
        assertThat(number.intValue()).isEqualTo(0);
    }
}
