// src/test/java/com/edutech/assess/AssessControllerIT.java
package com.edutech.assess;

import com.edutech.assess.application.dto.AddQuestionRequest;
import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.CreateExamRequest;
import com.edutech.assess.application.dto.ExamResponse;
import com.edutech.assess.application.dto.QuestionResponse;
import com.edutech.assess.application.dto.SubmissionResponse;
import com.edutech.assess.domain.model.ExamMode;
import com.edutech.assess.domain.model.ExamStatus;
import com.edutech.assess.domain.model.Role;
import com.edutech.assess.domain.model.SubmissionStatus;
import com.edutech.assess.infrastructure.security.JwtTokenValidator;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HTTP-level integration tests for assess-svc controllers.
 *
 * <p>Uses a real PostgreSQL container via TestContainers with Flyway migrations
 * applied automatically. Kafka and the JWT validator are mocked — the validator
 * is configured to accept any bearer token string and return a pre-built
 * {@link AuthPrincipal} so that security does not block requests.
 *
 * <p>Test coverage:
 * <ul>
 *   <li>POST  /api/v1/exams          — creates exam, returns 201 with correct body</li>
 *   <li>GET   /api/v1/exams/{id}     — retrieves exam by id</li>
 *   <li>PUT   /api/v1/exams/{id}/publish — transitions DRAFT → PUBLISHED</li>
 *   <li>POST  /api/v1/exams/{id}/questions — adds question to exam</li>
 *   <li>POST  /api/v1/exams/{id}/enrollments — enrolls student</li>
 *   <li>POST  /api/v1/submissions/{examId}/start — starts submission</li>
 *   <li>GET   /api/v1/exams (no auth) — returns 401</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("assess-svc — Controller HTTP Integration Tests")
class AssessControllerIT {

    // -------------------------------------------------------------------------
    // Infrastructure: single shared pgvector container (reused across tests)
    // -------------------------------------------------------------------------

    /**
     * pgvector image is required because Flyway V8__activate_pgvector.sql runs
     * "CREATE EXTENSION IF NOT EXISTS vector", which needs the pgvector shared
     * library present in the PostgreSQL installation.
     */
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("assess_ctrl_test")
                    .withUsername("assess_user")
                    .withPassword("assess_pass");

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

    /**
     * Mock the JWT validator so any bearer token value returns a known principal.
     * Individual tests override this per-principal via {@link #mockAuth(AuthPrincipal)}.
     */
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

    static final UUID CENTER_ID  = UUID.randomUUID();
    static final UUID BATCH_ID   = UUID.randomUUID();
    static final UUID TEACHER_ID = UUID.randomUUID();
    static final UUID STUDENT_ID = UUID.randomUUID();
    static final UUID ADMIN_ID   = UUID.randomUUID();

    static final String FAKE_TOKEN = "test-token";

    AuthPrincipal adminPrincipal;
    AuthPrincipal teacherPrincipal;
    AuthPrincipal studentPrincipal;

    @BeforeEach
    void setUp() {
        adminPrincipal  = new AuthPrincipal(ADMIN_ID,   "admin@test.com",   Role.SUPER_ADMIN,  CENTER_ID, "fp-admin");
        teacherPrincipal = new AuthPrincipal(TEACHER_ID, "teacher@test.com", Role.TEACHER,      CENTER_ID, "fp-teacher");
        studentPrincipal = new AuthPrincipal(STUDENT_ID, "student@test.com", Role.STUDENT,      null,      "fp-student");

        // Default auth: admin
        mockAuth(adminPrincipal);

        // Kafka mock: return completed futures to avoid blocking
        when(kafkaTemplate.send(anyString(), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Configures the JWT mock to return the given principal for any token. */
    private void mockAuth(AuthPrincipal principal) {
        when(jwtTokenValidator.validate(anyString())).thenReturn(Optional.of(principal));
    }

    /** Builds an Authorization header carrying the fake bearer token. */
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

    /** Builds a minimal valid CreateExamRequest. */
    private CreateExamRequest buildExamRequest(String title) {
        return new CreateExamRequest(
                title,
                "Controller IT exam",
                BATCH_ID,
                CENTER_ID,
                ExamMode.STANDARD,
                60,
                2,
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                100.0,
                40.0
        );
    }

    // =========================================================================
    // Test 1: POST /api/v1/exams — creates exam, returns HTTP 201
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/exams — returns 201 Created with DRAFT exam body")
    void createExam_returns201WithDraftStatus() {
        CreateExamRequest request = buildExamRequest("Controller IT — Create Test");

        ResponseEntity<ExamResponse> response = restTemplate.exchange(
                "/api/v1/exams",
                HttpMethod.POST,
                authEntity(request),
                ExamResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ExamResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.title()).isEqualTo("Controller IT — Create Test");
        assertThat(body.status()).isEqualTo(ExamStatus.DRAFT);
        assertThat(body.batchId()).isEqualTo(BATCH_ID);
        assertThat(body.centerId()).isEqualTo(CENTER_ID);
        assertThat(body.durationMinutes()).isEqualTo(60);
        assertThat(body.totalMarks()).isEqualTo(100.0);
        assertThat(body.passingMarks()).isEqualTo(40.0);
    }

    // =========================================================================
    // Test 2: GET /api/v1/exams/{examId} — retrieves exam by ID
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/exams/{id} — returns 200 with correct exam details")
    void getExam_returns200WithExamDetails() {
        // Create an exam first
        ResponseEntity<ExamResponse> created = restTemplate.exchange(
                "/api/v1/exams",
                HttpMethod.POST,
                authEntity(buildExamRequest("Controller IT — Get Test")),
                ExamResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID examId = created.getBody().id();

        // Retrieve it
        ResponseEntity<ExamResponse> response = restTemplate.exchange(
                "/api/v1/exams/" + examId,
                HttpMethod.GET,
                authEntity(),
                ExamResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ExamResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(examId);
        assertThat(body.title()).isEqualTo("Controller IT — Get Test");
        assertThat(body.status()).isEqualTo(ExamStatus.DRAFT);
    }

    // =========================================================================
    // Test 3: PUT /api/v1/exams/{examId}/publish — transitions DRAFT to PUBLISHED
    // =========================================================================

    @Test
    @DisplayName("PUT /api/v1/exams/{id}/publish — returns 200 with PUBLISHED status")
    void publishExam_returns200WithPublishedStatus() {
        // Create
        ResponseEntity<ExamResponse> created = restTemplate.exchange(
                "/api/v1/exams",
                HttpMethod.POST,
                authEntity(buildExamRequest("Controller IT — Publish Test")),
                ExamResponse.class);
        UUID examId = created.getBody().id();
        assertThat(created.getBody().status()).isEqualTo(ExamStatus.DRAFT);

        // Publish
        ResponseEntity<ExamResponse> published = restTemplate.exchange(
                "/api/v1/exams/" + examId + "/publish",
                HttpMethod.PUT,
                authEntity(),
                ExamResponse.class);

        assertThat(published.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(published.getBody()).isNotNull();
        assertThat(published.getBody().status()).isEqualTo(ExamStatus.PUBLISHED);
        assertThat(published.getBody().id()).isEqualTo(examId);
    }

    // =========================================================================
    // Test 4: POST /api/v1/exams/{examId}/questions — adds a question to exam
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/exams/{id}/questions — returns 201 with question linked to exam")
    void addQuestion_returns201WithQuestionBody() {
        // Create exam
        UUID examId = restTemplate.exchange(
                "/api/v1/exams",
                HttpMethod.POST,
                authEntity(buildExamRequest("Controller IT — Question Test")),
                ExamResponse.class).getBody().id();

        AddQuestionRequest qReq = new AddQuestionRequest(
                "What is the speed of light?",
                List.of("3×10⁸ m/s", "3×10⁶ m/s", "3×10¹⁰ m/s", "3×10⁴ m/s"),
                0, // correct: first option
                "Speed of light in vacuum is approximately 3×10⁸ m/s",
                5.0,
                0.3,
                0.8,
                0.1
        );

        // Teacher principal for question authoring (belongs to same CENTER_ID)
        mockAuth(teacherPrincipal);

        ResponseEntity<QuestionResponse> response = restTemplate.exchange(
                "/api/v1/exams/" + examId + "/questions",
                HttpMethod.POST,
                authEntity(qReq),
                QuestionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        QuestionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.examId()).isEqualTo(examId);
        assertThat(body.questionText()).isEqualTo("What is the speed of light?");
        assertThat(body.correctAnswer()).isEqualTo(0);
        assertThat(body.marks()).isEqualTo(5.0);
    }

    // =========================================================================
    // Test 5: Full flow — enroll student (self-enrollment) and start submission
    //
    // EnrollmentController always uses the authenticated principal's userId —
    // it ignores any studentId in the request body. So we must call the enroll
    // endpoint as the student principal to self-enroll.
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/exams/{examId}/submissions — returns 201 IN_PROGRESS after self-enrollment")
    void startSubmission_returns201WithInProgressStatus() {
        // Step 1: Create and publish exam as admin
        mockAuth(adminPrincipal);
        UUID examId = restTemplate.exchange(
                "/api/v1/exams",
                HttpMethod.POST,
                authEntity(buildExamRequest("Controller IT — Submission Test")),
                ExamResponse.class).getBody().id();

        restTemplate.exchange(
                "/api/v1/exams/" + examId + "/publish",
                HttpMethod.PUT,
                authEntity(),
                ExamResponse.class);

        // Step 2: Self-enroll as student (controller uses principal.userId())
        mockAuth(studentPrincipal);
        ResponseEntity<Map> enrollResponse = restTemplate.exchange(
                "/api/v1/exams/" + examId + "/enrollments",
                HttpMethod.POST,
                authEntity(),   // no body — controller ignores it for students
                Map.class);
        assertThat(enrollResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Step 3: Start submission as student
        ResponseEntity<SubmissionResponse> submissionResponse = restTemplate.exchange(
                "/api/v1/exams/" + examId + "/submissions",
                HttpMethod.POST,
                authEntity(),
                SubmissionResponse.class);

        assertThat(submissionResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SubmissionResponse submission = submissionResponse.getBody();
        assertThat(submission).isNotNull();
        assertThat(submission.id()).isNotNull();
        assertThat(submission.examId()).isEqualTo(examId);
        assertThat(submission.studentId()).isEqualTo(STUDENT_ID);
        assertThat(submission.status()).isEqualTo(SubmissionStatus.IN_PROGRESS);
        assertThat(submission.attemptNumber()).isEqualTo(1);
        assertThat(submission.startedAt()).isNotNull();
    }

    // =========================================================================
    // Test 6: GET /api/v1/exams without Authorization header → 401
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/exams without auth header — returns 401 Unauthorized")
    void listExams_withoutAuth_returns401() {
        // Configure validator to return empty (no principal) when no token present
        when(jwtTokenValidator.validate(anyString())).thenReturn(Optional.empty());

        // Send request with no Authorization header at all
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/exams",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // =========================================================================
    // Test 7: GET /api/v1/exams/{unknownId} → 404
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/exams/{unknownId} — returns 404 for non-existent exam")
    void getExam_unknownId_returns404() {
        UUID unknown = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/exams/" + unknown,
                HttpMethod.GET,
                authEntity(),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // =========================================================================
    // Test 8: POST /api/v1/exams with blank title → 400
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/exams with blank title — returns 422 Unprocessable Entity (validation error)")
    void createExam_blankTitle_returns422() {
        CreateExamRequest invalid = new CreateExamRequest(
                "",          // blank title — @NotBlank violation
                null,
                BATCH_ID,
                CENTER_ID,
                ExamMode.STANDARD,
                60,
                1,
                null, null,
                50.0, 20.0
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/exams",
                HttpMethod.POST,
                authEntity(invalid),
                String.class);

        // GlobalExceptionHandler maps MethodArgumentNotValidException to 422
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
