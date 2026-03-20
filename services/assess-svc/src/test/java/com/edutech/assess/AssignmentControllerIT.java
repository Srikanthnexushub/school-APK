// src/test/java/com/edutech/assess/AssignmentControllerIT.java
package com.edutech.assess;

import com.edutech.assess.application.dto.AssignmentResponse;
import com.edutech.assess.application.dto.AssignmentSubmissionResponse;
import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.CreateAssignmentRequest;
import com.edutech.assess.application.dto.GradeSubmissionRequest;
import com.edutech.assess.application.dto.SubmitAssignmentRequest;
import com.edutech.assess.application.dto.UpdateAssignmentRequest;
import com.edutech.assess.domain.model.AssignmentStatus;
import com.edutech.assess.domain.model.AssignmentSubmissionStatus;
import com.edutech.assess.domain.model.AssignmentType;
import com.edutech.assess.domain.model.Role;
import com.edutech.assess.infrastructure.security.JwtTokenValidator;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HTTP-level integration tests for {@link com.edutech.assess.api.AssignmentController}.
 *
 * <p>Uses a real PostgreSQL (pgvector) container via TestContainers with Flyway migrations
 * applied automatically. Kafka and the JWT validator are mocked — the validator returns a
 * pre-configured {@link AuthPrincipal} for any bearer token so that Spring Security does
 * not block requests.
 *
 * <p>Test coverage:
 * <ul>
 *   <li>POST   /api/v1/assignments                              — TEACHER creates, returns 201 DRAFT</li>
 *   <li>POST   /api/v1/assignments                              — STUDENT forbidden, returns 403</li>
 *   <li>POST   /api/v1/assignments                              — CENTER_ADMIN creates, returns 201</li>
 *   <li>GET    /api/v1/assignments/{id}                         — TEACHER owner, returns 200</li>
 *   <li>GET    /api/v1/assignments/{id}                         — STUDENT: 403 on DRAFT, 200 after publish</li>
 *   <li>GET    /api/v1/assignments?batchId={id}                 — TEACHER sees all, list size >= 2</li>
 *   <li>GET    /api/v1/assignments?batchId={id}                 — STUDENT sees only PUBLISHED</li>
 *   <li>PUT    /api/v1/assignments/{id}                         — TEACHER update title, returns 200</li>
 *   <li>PUT    /api/v1/assignments/{id}                         — wrong center, returns 403</li>
 *   <li>PATCH  /api/v1/assignments/{id}/publish                 — returns PUBLISHED</li>
 *   <li>PATCH  /api/v1/assignments/{id}/close                   — returns CLOSED</li>
 *   <li>DELETE /api/v1/assignments/{id}                         — 204, GET afterwards 404</li>
 *   <li>POST   /api/v1/assignments/{id}/submissions             — STUDENT submits, returns 201 SUBMITTED</li>
 *   <li>POST   /api/v1/assignments/{id}/submissions (late)      — past dueDate, returns LATE</li>
 *   <li>POST   /api/v1/assignments/{id}/submissions             — TEACHER forbidden, returns 403</li>
 *   <li>GET    /api/v1/assignments/{id}/submissions             — TEACHER sees all submissions</li>
 *   <li>PATCH  /api/v1/assignments/{id}/submissions/{subId}/grade — TEACHER grades, returns GRADED</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("assess-svc — AssignmentController HTTP Integration Tests")
class AssignmentControllerIT {

    // -------------------------------------------------------------------------
    // Infrastructure: single shared pgvector container
    // -------------------------------------------------------------------------

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("assess_assign_test")
                    .withUsername("assign_user")
                    .withPassword("assign_pass");

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

    static final UUID CENTER_A   = UUID.randomUUID();
    static final UUID CENTER_B   = UUID.randomUUID();
    static final UUID BATCH_ID   = UUID.randomUUID();
    static final UUID TEACHER_ID = UUID.randomUUID();
    static final UUID STUDENT_ID = UUID.randomUUID();
    static final UUID ADMIN_ID   = UUID.randomUUID();

    static final String FAKE_TOKEN = "test-token";

    AuthPrincipal adminPrincipal;
    AuthPrincipal teacherPrincipal;
    AuthPrincipal studentPrincipal;
    AuthPrincipal centerBTeacherPrincipal;

    @BeforeEach
    void setUp() {
        adminPrincipal   = new AuthPrincipal(ADMIN_ID,   "admin@test.com",    Role.SUPER_ADMIN, null,     "fp-admin");
        teacherPrincipal = new AuthPrincipal(TEACHER_ID, "teacher@test.com",  Role.TEACHER,     CENTER_A, "fp-teacher");
        studentPrincipal = new AuthPrincipal(STUDENT_ID, "student@test.com",  Role.STUDENT,     null,     "fp-student");
        centerBTeacherPrincipal = new AuthPrincipal(UUID.randomUUID(), "teacher.b@test.com",
                Role.TEACHER, CENTER_B, "fp-teacher-b");

        // Default auth: teacher for CENTER_A
        mockAuth(teacherPrincipal);

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
     * Builds a minimal valid CreateAssignmentRequest for CENTER_A.
     */
    private CreateAssignmentRequest buildRequest(String title, UUID batchId, UUID centerId) {
        return new CreateAssignmentRequest(
                batchId,
                centerId,
                title,
                "Integration test assignment",
                AssignmentType.HOMEWORK,
                Instant.now().plusSeconds(86400), // due in 24 h
                100.0,
                40.0,
                "Complete all sections",
                null
        );
    }

    /**
     * Creates an assignment as the current mock principal and returns its UUID.
     */
    private UUID createAssignment(String title) {
        ResponseEntity<AssignmentResponse> resp = restTemplate.exchange(
                "/api/v1/assignments",
                HttpMethod.POST,
                authEntity(buildRequest(title, BATCH_ID, CENTER_A)),
                AssignmentResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    /**
     * Publishes an assignment (as teacher principal) and returns the updated response.
     */
    private AssignmentResponse publishAssignment(UUID id) {
        mockAuth(teacherPrincipal);
        ResponseEntity<AssignmentResponse> resp = restTemplate.exchange(
                "/api/v1/assignments/" + id + "/publish",
                HttpMethod.PATCH,
                authEntity(),
                AssignmentResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    // =========================================================================
    // Test 1: POST /api/v1/assignments as TEACHER → 201, status=DRAFT
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/assignments — TEACHER creates assignment → 201, status=DRAFT")
    void createAssignment_teacher_succeeds() {
        mockAuth(teacherPrincipal);

        ResponseEntity<AssignmentResponse> response = restTemplate.exchange(
                "/api/v1/assignments",
                HttpMethod.POST,
                authEntity(buildRequest("Algebra Homework", BATCH_ID, CENTER_A)),
                AssignmentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AssignmentResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.title()).isEqualTo("Algebra Homework");
        assertThat(body.status()).isEqualTo(AssignmentStatus.DRAFT);
        assertThat(body.centerId()).isEqualTo(CENTER_A);
        assertThat(body.batchId()).isEqualTo(BATCH_ID);
        assertThat(body.totalMarks()).isEqualTo(100.0);
        assertThat(body.passingMarks()).isEqualTo(40.0);
    }

    // =========================================================================
    // Test 2: POST /api/v1/assignments as STUDENT → 403
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/assignments — STUDENT creates assignment → 403 Forbidden")
    void createAssignment_student_forbidden() {
        mockAuth(studentPrincipal);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/assignments",
                HttpMethod.POST,
                authEntity(buildRequest("Student Should Not Post", BATCH_ID, CENTER_A)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // Test 3: POST /api/v1/assignments as CENTER_ADMIN → 201
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/assignments — CENTER_ADMIN creates assignment → 201")
    void createAssignment_centerAdmin_succeeds() {
        AuthPrincipal centerAdminPrincipal = new AuthPrincipal(
                UUID.randomUUID(), "admin@center.com", Role.CENTER_ADMIN, CENTER_A, "fp-center-admin");
        mockAuth(centerAdminPrincipal);

        ResponseEntity<AssignmentResponse> response = restTemplate.exchange(
                "/api/v1/assignments",
                HttpMethod.POST,
                authEntity(buildRequest("Admin Project Assignment", BATCH_ID, CENTER_A)),
                AssignmentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AssignmentResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.centerId()).isEqualTo(CENTER_A);
    }

    // =========================================================================
    // Test 4: GET /api/v1/assignments/{id} as TEACHER owner → 200 + title matches
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/assignments/{id} — TEACHER owner retrieves own assignment → 200, title matches")
    void getAssignment_owner_succeeds() {
        mockAuth(teacherPrincipal);
        UUID id = createAssignment("Science Worksheet");

        ResponseEntity<AssignmentResponse> response = restTemplate.exchange(
                "/api/v1/assignments/" + id,
                HttpMethod.GET,
                authEntity(),
                AssignmentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AssignmentResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(id);
        assertThat(body.title()).isEqualTo("Science Worksheet");
        assertThat(body.centerId()).isEqualTo(CENTER_A);
    }

    // =========================================================================
    // Test 5: GET /api/v1/assignments/{id} as STUDENT
    //         - DRAFT → 403; after publish → 200
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/assignments/{id} — STUDENT: 403 on DRAFT, 200 after publish")
    void getAssignment_student_onlyPublished() {
        mockAuth(teacherPrincipal);
        UUID id = createAssignment("Geography Essay");

        // As STUDENT — assignment is DRAFT → 403
        mockAuth(studentPrincipal);
        ResponseEntity<String> draftResponse = restTemplate.exchange(
                "/api/v1/assignments/" + id,
                HttpMethod.GET,
                authEntity(),
                String.class);
        assertThat(draftResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Publish as teacher
        publishAssignment(id);

        // As STUDENT — assignment is now PUBLISHED → 200
        mockAuth(studentPrincipal);
        ResponseEntity<AssignmentResponse> publishedResponse = restTemplate.exchange(
                "/api/v1/assignments/" + id,
                HttpMethod.GET,
                authEntity(),
                AssignmentResponse.class);
        assertThat(publishedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publishedResponse.getBody()).isNotNull();
        assertThat(publishedResponse.getBody().id()).isEqualTo(id);
    }

    // =========================================================================
    // Test 6: GET /api/v1/assignments?batchId={id} as TEACHER → list size >= 2
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/assignments?batchId — TEACHER sees all assignments, size >= 2")
    void listAssignmentsByBatch_teacher_succeeds() {
        UUID batchForTest = UUID.randomUUID();
        mockAuth(teacherPrincipal);

        // Create two assignments for the same batch (using CENTER_A)
        CreateAssignmentRequest req1 = new CreateAssignmentRequest(
                batchForTest, CENTER_A, "Chemistry Lab Report " + UUID.randomUUID(),
                null, AssignmentType.CLASSWORK, Instant.now().plusSeconds(86400),
                50.0, 20.0, null, null);
        CreateAssignmentRequest req2 = new CreateAssignmentRequest(
                batchForTest, CENTER_A, "Physics Problem Set " + UUID.randomUUID(),
                null, AssignmentType.HOMEWORK, Instant.now().plusSeconds(86400),
                80.0, 32.0, null, null);

        restTemplate.exchange("/api/v1/assignments", HttpMethod.POST, authEntity(req1), AssignmentResponse.class);
        restTemplate.exchange("/api/v1/assignments", HttpMethod.POST, authEntity(req2), AssignmentResponse.class);

        ResponseEntity<List<AssignmentResponse>> response = restTemplate.exchange(
                "/api/v1/assignments?batchId=" + batchForTest,
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<AssignmentResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<AssignmentResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).hasSizeGreaterThanOrEqualTo(2);
    }

    // =========================================================================
    // Test 7: GET /api/v1/assignments?batchId={id} as STUDENT
    //         → sees only PUBLISHED, not DRAFT
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/assignments?batchId — STUDENT sees only PUBLISHED assignments")
    void listAssignmentsByBatch_student_seesPublishedOnly() {
        UUID batchForTest = UUID.randomUUID();
        mockAuth(teacherPrincipal);

        String draftTitle     = "Draft Assignment " + UUID.randomUUID();
        String publishedTitle = "Published Assignment " + UUID.randomUUID();

        // Create one DRAFT assignment
        CreateAssignmentRequest draftReq = new CreateAssignmentRequest(
                batchForTest, CENTER_A, draftTitle,
                null, AssignmentType.PROJECT, Instant.now().plusSeconds(86400),
                60.0, 24.0, null, null);
        restTemplate.exchange("/api/v1/assignments", HttpMethod.POST, authEntity(draftReq), AssignmentResponse.class);

        // Create one assignment and immediately publish it
        CreateAssignmentRequest pubReq = new CreateAssignmentRequest(
                batchForTest, CENTER_A, publishedTitle,
                null, AssignmentType.QUIZ, Instant.now().plusSeconds(86400),
                40.0, 16.0, null, null);
        ResponseEntity<AssignmentResponse> pubResp = restTemplate.exchange(
                "/api/v1/assignments", HttpMethod.POST, authEntity(pubReq), AssignmentResponse.class);
        assertThat(pubResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID pubId = pubResp.getBody().id();

        // Publish
        publishAssignment(pubId);

        // As STUDENT — list the batch
        mockAuth(studentPrincipal);
        ResponseEntity<List<AssignmentResponse>> response = restTemplate.exchange(
                "/api/v1/assignments?batchId=" + batchForTest,
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<AssignmentResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<AssignmentResponse> body = response.getBody();
        assertThat(body).isNotNull();

        List<String> titles = body.stream().map(AssignmentResponse::title).toList();
        assertThat(titles).contains(publishedTitle);
        assertThat(titles).doesNotContain(draftTitle);
    }

    // =========================================================================
    // Test 8: PUT /api/v1/assignments/{id} as TEACHER → 200, new title
    // =========================================================================

    @Test
    @DisplayName("PUT /api/v1/assignments/{id} — TEACHER updates title → 200, new title returned")
    void updateAssignment_teacher_succeeds() {
        mockAuth(teacherPrincipal);
        UUID id = createAssignment("Original Title");

        UpdateAssignmentRequest updateReq = new UpdateAssignmentRequest(
                "Updated Title",
                "Updated description",
                "Updated instructions",
                null,
                Instant.now().plusSeconds(172800),
                90.0,
                36.0
        );

        ResponseEntity<AssignmentResponse> response = restTemplate.exchange(
                "/api/v1/assignments/" + id,
                HttpMethod.PUT,
                authEntity(updateReq),
                AssignmentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AssignmentResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(id);
        assertThat(body.title()).isEqualTo("Updated Title");
        assertThat(body.description()).isEqualTo("Updated description");
        assertThat(body.totalMarks()).isEqualTo(90.0);
    }

    // =========================================================================
    // Test 9: PUT /api/v1/assignments/{id} by teacher from wrong center → 403
    // =========================================================================

    @Test
    @DisplayName("PUT /api/v1/assignments/{id} — teacher from different center → 403 Forbidden")
    void updateAssignment_wrongCenter_forbidden() {
        // Create assignment as CENTER_A teacher
        mockAuth(teacherPrincipal);
        UUID id = createAssignment("Center A Assignment");

        // Try update as CENTER_B teacher (different center, does not own it)
        mockAuth(centerBTeacherPrincipal);
        UpdateAssignmentRequest updateReq = new UpdateAssignmentRequest(
                "Should Not Update", null, null, null, null, 50.0, 20.0);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/assignments/" + id,
                HttpMethod.PUT,
                authEntity(updateReq),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // Test 10: PATCH /api/v1/assignments/{id}/publish → PUBLISHED
    // =========================================================================

    @Test
    @DisplayName("PATCH /api/v1/assignments/{id}/publish — DRAFT assignment → PUBLISHED")
    void publishAssignment_succeeds() {
        mockAuth(teacherPrincipal);
        UUID id = createAssignment("Assignment To Publish");

        ResponseEntity<AssignmentResponse> response = restTemplate.exchange(
                "/api/v1/assignments/" + id + "/publish",
                HttpMethod.PATCH,
                authEntity(),
                AssignmentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AssignmentResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(id);
        assertThat(body.status()).isEqualTo(AssignmentStatus.PUBLISHED);
    }

    // =========================================================================
    // Test 11: PATCH /api/v1/assignments/{id}/close → CLOSED
    // =========================================================================

    @Test
    @DisplayName("PATCH /api/v1/assignments/{id}/close — PUBLISHED assignment → CLOSED")
    void closeAssignment_succeeds() {
        mockAuth(teacherPrincipal);
        UUID id = createAssignment("Assignment To Close");

        // Publish first
        publishAssignment(id);

        // Now close
        mockAuth(teacherPrincipal);
        ResponseEntity<AssignmentResponse> response = restTemplate.exchange(
                "/api/v1/assignments/" + id + "/close",
                HttpMethod.PATCH,
                authEntity(),
                AssignmentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AssignmentResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(AssignmentStatus.CLOSED);
    }

    // =========================================================================
    // Test 12: DELETE /api/v1/assignments/{id} → 204, then GET → 404
    // =========================================================================

    @Test
    @DisplayName("DELETE /api/v1/assignments/{id} — soft-delete returns 204, GET returns 404")
    void deleteAssignment_succeeds() {
        mockAuth(teacherPrincipal);
        UUID id = createAssignment("Assignment To Delete");

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/assignments/" + id,
                HttpMethod.DELETE,
                authEntity(),
                Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/api/v1/assignments/" + id,
                HttpMethod.GET,
                authEntity(),
                String.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // =========================================================================
    // Test 13: POST /api/v1/assignments/{id}/submissions as STUDENT → 201, SUBMITTED
    // =========================================================================

    @Test
    @DisplayName("POST /assignments/{id}/submissions — STUDENT submits → 201, status=SUBMITTED")
    void submitAssignment_student_succeeds() {
        // Create and publish assignment
        mockAuth(teacherPrincipal);
        UUID id = createAssignment("Essay Assignment");
        publishAssignment(id);

        // Submit as student
        mockAuth(studentPrincipal);
        SubmitAssignmentRequest submitReq = new SubmitAssignmentRequest("My essay response text");

        ResponseEntity<AssignmentSubmissionResponse> response = restTemplate.exchange(
                "/api/v1/assignments/" + id + "/submissions",
                HttpMethod.POST,
                authEntity(submitReq),
                AssignmentSubmissionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AssignmentSubmissionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.assignmentId()).isEqualTo(id);
        assertThat(body.studentId()).isEqualTo(STUDENT_ID);
        assertThat(body.status()).isEqualTo(AssignmentSubmissionStatus.SUBMITTED);
        assertThat(body.textResponse()).isEqualTo("My essay response text");
        assertThat(body.submittedAt()).isNotNull();
    }

    // =========================================================================
    // Test 14: POST submission with past dueDate → status=LATE
    // =========================================================================

    @Test
    @DisplayName("POST /assignments/{id}/submissions — past dueDate → status=LATE")
    void submitAssignment_late_markedLate() {
        mockAuth(teacherPrincipal);

        // Create assignment with past dueDate
        CreateAssignmentRequest lateReq = new CreateAssignmentRequest(
                BATCH_ID,
                CENTER_A,
                "Late Submission Assignment " + UUID.randomUUID(),
                null,
                AssignmentType.PRACTICE,
                Instant.now().minusSeconds(3600), // dueDate 1 hour ago
                50.0,
                20.0,
                null,
                null
        );
        ResponseEntity<AssignmentResponse> createResp = restTemplate.exchange(
                "/api/v1/assignments", HttpMethod.POST, authEntity(lateReq), AssignmentResponse.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID id = createResp.getBody().id();

        // Publish
        publishAssignment(id);

        // Submit late as student
        mockAuth(studentPrincipal);
        SubmitAssignmentRequest submitReq = new SubmitAssignmentRequest("Late submission text");

        ResponseEntity<AssignmentSubmissionResponse> response = restTemplate.exchange(
                "/api/v1/assignments/" + id + "/submissions",
                HttpMethod.POST,
                authEntity(submitReq),
                AssignmentSubmissionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AssignmentSubmissionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(AssignmentSubmissionStatus.LATE);
    }

    // =========================================================================
    // Test 15: POST /assignments/{id}/submissions as TEACHER → 403
    // =========================================================================

    @Test
    @DisplayName("POST /assignments/{id}/submissions — TEACHER submits → 403 Forbidden")
    void submitAssignment_nonStudent_forbidden() {
        mockAuth(teacherPrincipal);
        UUID id = createAssignment("Teacher Cannot Submit");
        publishAssignment(id);

        mockAuth(teacherPrincipal);
        SubmitAssignmentRequest submitReq = new SubmitAssignmentRequest("Teacher text");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/assignments/" + id + "/submissions",
                HttpMethod.POST,
                authEntity(submitReq),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // Test 16: GET /api/v1/assignments/{id}/submissions as TEACHER → list not empty
    // =========================================================================

    @Test
    @DisplayName("GET /assignments/{id}/submissions — TEACHER lists submissions → list not empty")
    void listSubmissions_teacher_succeeds() {
        // Create, publish
        mockAuth(teacherPrincipal);
        UUID id = createAssignment("Submission List Assignment");
        publishAssignment(id);

        // Student submits
        mockAuth(studentPrincipal);
        restTemplate.exchange(
                "/api/v1/assignments/" + id + "/submissions",
                HttpMethod.POST,
                authEntity(new SubmitAssignmentRequest("Student answer text")),
                AssignmentSubmissionResponse.class);

        // Teacher lists submissions
        mockAuth(teacherPrincipal);
        ResponseEntity<List<AssignmentSubmissionResponse>> response = restTemplate.exchange(
                "/api/v1/assignments/" + id + "/submissions",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<AssignmentSubmissionResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<AssignmentSubmissionResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).isNotEmpty();
        assertThat(body.get(0).assignmentId()).isEqualTo(id);
    }

    // =========================================================================
    // Test 17: PATCH /assignments/{id}/submissions/{subId}/grade → GRADED
    // =========================================================================

    @Test
    @DisplayName("PATCH /assignments/{id}/submissions/{subId}/grade — TEACHER grades → status=GRADED")
    void gradeSubmission_teacher_succeeds() {
        // Create, publish
        mockAuth(teacherPrincipal);
        UUID id = createAssignment("Gradeable Assignment");
        publishAssignment(id);

        // Student submits
        mockAuth(studentPrincipal);
        ResponseEntity<AssignmentSubmissionResponse> submitResp = restTemplate.exchange(
                "/api/v1/assignments/" + id + "/submissions",
                HttpMethod.POST,
                authEntity(new SubmitAssignmentRequest("My answer")),
                AssignmentSubmissionResponse.class);
        assertThat(submitResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID subId = submitResp.getBody().id();

        // Teacher gets submission list (to confirm subId)
        mockAuth(teacherPrincipal);
        ResponseEntity<List<AssignmentSubmissionResponse>> listResp = restTemplate.exchange(
                "/api/v1/assignments/" + id + "/submissions",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<AssignmentSubmissionResponse>>() {});
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).isNotEmpty();

        // Teacher grades the submission
        GradeSubmissionRequest gradeReq = new GradeSubmissionRequest(85.0, "Excellent work!");

        ResponseEntity<AssignmentSubmissionResponse> gradeResp = restTemplate.exchange(
                "/api/v1/assignments/" + id + "/submissions/" + subId + "/grade",
                HttpMethod.PATCH,
                authEntity(gradeReq),
                AssignmentSubmissionResponse.class);

        assertThat(gradeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        AssignmentSubmissionResponse body = gradeResp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(subId);
        assertThat(body.status()).isEqualTo(AssignmentSubmissionStatus.GRADED);
        assertThat(body.score()).isEqualTo(85.0);
        assertThat(body.feedback()).isEqualTo("Excellent work!");
        assertThat(body.gradedAt()).isNotNull();
    }
}
