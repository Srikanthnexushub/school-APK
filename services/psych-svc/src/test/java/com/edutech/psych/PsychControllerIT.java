package com.edutech.psych;

import com.edutech.psych.application.dto.CompleteSessionRequest;
import com.edutech.psych.application.dto.CreatePsychProfileRequest;
import com.edutech.psych.application.dto.PsychProfileResponse;
import com.edutech.psych.application.dto.SessionResponse;
import com.edutech.psych.application.dto.StartSessionRequest;
import com.edutech.psych.domain.model.ProfileStatus;
import com.edutech.psych.domain.model.SessionStatus;
import com.edutech.psych.domain.model.SessionType;
import com.edutech.psych.infrastructure.security.JwtTokenValidator;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP-layer integration tests for psych-svc controllers.
 *
 * Strategy:
 *   - RANDOM_PORT starts the full servlet stack against a real PostgreSQL DB
 *     (PostgreSQLContainer with full Flyway migration).
 *   - KafkaContainer satisfies spring-kafka auto-configuration.
 *   - JwtTokenValidator is @MockBean because its constructor reads an RSA key
 *     from the filesystem, which is unavailable in test.
 *   - Authentication uses the ServiceKeyAuthFilter: sending the header
 *     X-Service-Key: test-service-key (from application-test.yml) injects a
 *     synthetic SUPER_ADMIN principal without a real JWT, so all ownership
 *     checks pass cleanly.
 *   - Tests cover: create profile, get profile, list by student, start session,
 *     complete session, 404 on unknown profile, 409 on double-complete.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class PsychControllerIT {

    // ---------------------------------------------------------------------------
    // Infrastructure containers — shared across all tests in this class
    // ---------------------------------------------------------------------------

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("psych_db_it")
                    .withUsername("psych_user")
                    .withPassword("psych_pass");

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

    /**
     * JwtTokenValidator's constructor tries to read an RSA public key from disk.
     * @MockBean replaces it with a no-op stub so the application context starts.
     * Actual HTTP authentication is satisfied via X-Service-Key (ServiceKeyAuthFilter).
     */
    @MockBean
    JwtTokenValidator jwtTokenValidator;

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /**
     * Build headers that use the service-key mechanism (application-test.yml:
     * service.api-key = test-service-key). The ServiceKeyAuthFilter injects a
     * SUPER_ADMIN AuthPrincipal, so no JWT is needed.
     */
    private HttpHeaders serviceKeyHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Service-Key", "test-service-key");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Create a fresh psych profile for a random student and return the response.
     * Uses SUPER_ADMIN (service key), so centerId ownership check is skipped.
     */
    private PsychProfileResponse createProfile(UUID studentId, UUID centerId, UUID batchId) {
        CreatePsychProfileRequest req = new CreatePsychProfileRequest(studentId, centerId, batchId);
        ResponseEntity<PsychProfileResponse> r = restTemplate.exchange(
                "/api/v1/psych/profiles",
                HttpMethod.POST,
                new HttpEntity<>(req, serviceKeyHeaders()),
                PsychProfileResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody();
    }

    /**
     * Start a session on the given profile and return the response.
     * SessionService.startSession checks principal.userId() == profile.studentId() OR super-admin.
     * Because X-Service-Key yields SUPER_ADMIN, this passes.
     */
    private SessionResponse startSession(UUID profileId, SessionType type) {
        StartSessionRequest req = new StartSessionRequest(type, Instant.now().plusSeconds(300));
        ResponseEntity<SessionResponse> r = restTemplate.exchange(
                "/api/v1/psych/profiles/" + profileId + "/sessions",
                HttpMethod.POST,
                new HttpEntity<>(req, serviceKeyHeaders()),
                SessionResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody();
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/psych/profiles — 201 with ACTIVE profile and zeroed Big-Five traits")
    void createPsychProfile_returns201_withActiveStatus() {
        UUID studentId = UUID.randomUUID();
        UUID centerId  = UUID.randomUUID();
        UUID batchId   = UUID.randomUUID();

        CreatePsychProfileRequest req = new CreatePsychProfileRequest(studentId, centerId, batchId);

        ResponseEntity<PsychProfileResponse> response = restTemplate.exchange(
                "/api/v1/psych/profiles",
                HttpMethod.POST,
                new HttpEntity<>(req, serviceKeyHeaders()),
                PsychProfileResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PsychProfileResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.studentId()).isEqualTo(studentId);
        assertThat(body.centerId()).isEqualTo(centerId);
        assertThat(body.batchId()).isEqualTo(batchId);
        assertThat(body.status()).isEqualTo(ProfileStatus.ACTIVE);
        // Initial Big-Five scores must all be zero
        assertThat(body.openness()).isEqualTo(0.0);
        assertThat(body.conscientiousness()).isEqualTo(0.0);
        assertThat(body.extraversion()).isEqualTo(0.0);
        assertThat(body.agreeableness()).isEqualTo(0.0);
        assertThat(body.neuroticism()).isEqualTo(0.0);
        assertThat(body.riasecCode()).isNull();
        assertThat(body.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("GET /api/v1/psych/profiles/{profileId} — 200 returns existing profile")
    void getProfile_returns200_forExistingProfile() {
        UUID studentId = UUID.randomUUID();
        UUID centerId  = UUID.randomUUID();
        UUID batchId   = UUID.randomUUID();

        PsychProfileResponse created = createProfile(studentId, centerId, batchId);

        ResponseEntity<PsychProfileResponse> response = restTemplate.exchange(
                "/api/v1/psych/profiles/" + created.id(),
                HttpMethod.GET,
                new HttpEntity<>(serviceKeyHeaders()),
                PsychProfileResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PsychProfileResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(created.id());
        assertThat(body.studentId()).isEqualTo(studentId);
        assertThat(body.status()).isEqualTo(ProfileStatus.ACTIVE);
    }

    @Test
    @DisplayName("GET /api/v1/psych/profiles/{profileId} — 404 for unknown profile id")
    void getProfile_returns404_forNonExistentProfile() {
        UUID nonExistent = UUID.randomUUID();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/psych/profiles/" + nonExistent,
                HttpMethod.GET,
                new HttpEntity<>(serviceKeyHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("title")).isEqualTo("Psych Profile Not Found");
    }

    @Test
    @DisplayName("GET /api/v1/psych/profiles?studentId=... — 200 returns list of profiles for student")
    void listByStudentId_returns200_allProfiles() {
        UUID studentId    = UUID.randomUUID();
        UUID centerId1    = UUID.randomUUID();
        UUID centerId2    = UUID.randomUUID();
        UUID batchId1     = UUID.randomUUID();
        UUID batchId2     = UUID.randomUUID();

        createProfile(studentId, centerId1, batchId1);
        createProfile(studentId, centerId2, batchId2);

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/v1/psych/profiles?studentId=" + studentId,
                HttpMethod.GET,
                new HttpEntity<>(serviceKeyHeaders()),
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> body = response.getBody();
        assertThat(body).isNotNull().hasSize(2);
    }

    @Test
    @DisplayName("POST /api/v1/psych/profiles/{profileId}/sessions — 201 session IN_PROGRESS")
    void startSession_returns201_withInProgressStatus() {
        UUID studentId = UUID.randomUUID();
        UUID centerId  = UUID.randomUUID();
        UUID batchId   = UUID.randomUUID();

        PsychProfileResponse profile = createProfile(studentId, centerId, batchId);

        StartSessionRequest req = new StartSessionRequest(
                SessionType.INITIAL,
                Instant.now().plusSeconds(600)
        );

        ResponseEntity<SessionResponse> response = restTemplate.exchange(
                "/api/v1/psych/profiles/" + profile.id() + "/sessions",
                HttpMethod.POST,
                new HttpEntity<>(req, serviceKeyHeaders()),
                SessionResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SessionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.profileId()).isEqualTo(profile.id());
        assertThat(body.studentId()).isEqualTo(studentId);
        assertThat(body.sessionType()).isEqualTo(SessionType.INITIAL);
        assertThat(body.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(body.startedAt()).isNotNull();
        assertThat(body.completedAt()).isNull();
    }

    @Test
    @DisplayName("POST /api/v1/psych/profiles/{profileId}/sessions/{sessionId}/complete — 200 updates traits")
    void completeSession_returns200_andUpdatesBigFiveTraits() {
        UUID studentId = UUID.randomUUID();
        UUID centerId  = UUID.randomUUID();
        UUID batchId   = UUID.randomUUID();

        PsychProfileResponse profile = createProfile(studentId, centerId, batchId);
        SessionResponse session = startSession(profile.id(), SessionType.INITIAL);

        CompleteSessionRequest completeReq = new CompleteSessionRequest(
                0.82,   // openness
                0.75,   // conscientiousness
                0.60,   // extraversion
                0.70,   // agreeableness
                0.25,   // neuroticism
                "RIA",
                "Strong analytical profile."
        );

        ResponseEntity<SessionResponse> response = restTemplate.exchange(
                "/api/v1/psych/profiles/" + profile.id() + "/sessions/" + session.id() + "/complete",
                HttpMethod.POST,
                new HttpEntity<>(completeReq, serviceKeyHeaders()),
                SessionResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SessionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(body.completedAt()).isNotNull();
        assertThat(body.notes()).isEqualTo("Strong analytical profile.");

        // Verify trait scores propagated to the profile
        ResponseEntity<PsychProfileResponse> profileAfter = restTemplate.exchange(
                "/api/v1/psych/profiles/" + profile.id(),
                HttpMethod.GET,
                new HttpEntity<>(serviceKeyHeaders()),
                PsychProfileResponse.class
        );
        assertThat(profileAfter.getStatusCode()).isEqualTo(HttpStatus.OK);
        PsychProfileResponse updatedProfile = profileAfter.getBody();
        assertThat(updatedProfile.openness()).isEqualTo(0.82);
        assertThat(updatedProfile.conscientiousness()).isEqualTo(0.75);
        assertThat(updatedProfile.extraversion()).isEqualTo(0.60);
        assertThat(updatedProfile.agreeableness()).isEqualTo(0.70);
        assertThat(updatedProfile.neuroticism()).isEqualTo(0.25);
        assertThat(updatedProfile.riasecCode()).isEqualTo("RIA");
    }

    @Test
    @DisplayName("POST …/sessions/{sessionId}/complete — 409 when session is already COMPLETED")
    void completeSession_returns409_whenAlreadyCompleted() {
        UUID studentId = UUID.randomUUID();
        UUID centerId  = UUID.randomUUID();
        UUID batchId   = UUID.randomUUID();

        PsychProfileResponse profile = createProfile(studentId, centerId, batchId);
        SessionResponse session = startSession(profile.id(), SessionType.PERIODIC);

        CompleteSessionRequest completeReq = new CompleteSessionRequest(
                0.5, 0.5, 0.5, 0.5, 0.5, "RI", "First complete"
        );

        // Complete once — should succeed
        ResponseEntity<SessionResponse> first = restTemplate.exchange(
                "/api/v1/psych/profiles/" + profile.id() + "/sessions/" + session.id() + "/complete",
                HttpMethod.POST,
                new HttpEntity<>(completeReq, serviceKeyHeaders()),
                SessionResponse.class
        );
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Complete again — must return 409
        ResponseEntity<Map> second = restTemplate.exchange(
                "/api/v1/psych/profiles/" + profile.id() + "/sessions/" + session.id() + "/complete",
                HttpMethod.POST,
                new HttpEntity<>(new CompleteSessionRequest(0.6, 0.6, 0.6, 0.6, 0.6, "RIA", "Second attempt"),
                        serviceKeyHeaders()),
                Map.class
        );
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().get("title")).isEqualTo("Session Already Completed");
    }

    @Test
    @DisplayName("GET …/sessions — 200 returns paginated session list for the profile")
    void listSessions_returns200_pageOfSessions() {
        UUID studentId = UUID.randomUUID();
        UUID centerId  = UUID.randomUUID();
        UUID batchId   = UUID.randomUUID();

        PsychProfileResponse profile = createProfile(studentId, centerId, batchId);

        // Start two sessions
        SessionResponse s1 = startSession(profile.id(), SessionType.INITIAL);
        SessionResponse s2 = startSession(profile.id(), SessionType.PERIODIC);

        // Complete both
        CompleteSessionRequest cr = new CompleteSessionRequest(0.5, 0.5, 0.5, 0.5, 0.5, null, "ok");
        restTemplate.exchange("/api/v1/psych/profiles/" + profile.id() + "/sessions/" + s1.id() + "/complete",
                HttpMethod.POST, new HttpEntity<>(cr, serviceKeyHeaders()), SessionResponse.class);
        restTemplate.exchange("/api/v1/psych/profiles/" + profile.id() + "/sessions/" + s2.id() + "/complete",
                HttpMethod.POST, new HttpEntity<>(cr, serviceKeyHeaders()), SessionResponse.class);

        // List sessions — Spring Page<> serializes with "content" key
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/psych/profiles/" + profile.id() + "/sessions",
                HttpMethod.GET,
                new HttpEntity<>(serviceKeyHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();
        Object content = body.get("content");
        assertThat(content).isInstanceOf(java.util.List.class);
        assertThat(((java.util.List<?>) content)).hasSize(2);
    }

    @Test
    @DisplayName("GET /api/v1/psych/profiles — 400 when neither studentId nor centerId is provided")
    void listProfiles_returns400_withNoQueryParam() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/psych/profiles",
                HttpMethod.GET,
                new HttpEntity<>(serviceKeyHeaders()),
                Map.class
        );

        // Controller returns ResponseEntity.badRequest().build() when neither param present
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/v1/psych/profiles/{profileId}/sessions — 400 on missing required fields")
    void startSession_returns400_whenSessionTypeNull() {
        UUID studentId = UUID.randomUUID();
        UUID centerId  = UUID.randomUUID();
        UUID batchId   = UUID.randomUUID();

        PsychProfileResponse profile = createProfile(studentId, centerId, batchId);

        // Omit sessionType (null) — @NotNull validation should reject this
        String invalidBody = "{\"scheduledAt\":\"2026-12-01T10:00:00Z\"}";

        HttpHeaders headers = serviceKeyHeaders();
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/psych/profiles/" + profile.id() + "/sessions",
                HttpMethod.POST,
                new HttpEntity<>(invalidBody, headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
