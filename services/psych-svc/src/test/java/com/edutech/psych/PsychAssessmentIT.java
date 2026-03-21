package com.edutech.psych;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.application.dto.CompleteSessionRequest;
import com.edutech.psych.application.dto.CreatePsychProfileRequest;
import com.edutech.psych.application.dto.StartSessionRequest;
import com.edutech.psych.application.service.PsychProfileService;
import com.edutech.psych.application.service.SessionService;
import com.edutech.psych.domain.model.ProfileStatus;
import com.edutech.psych.domain.model.Role;
import com.edutech.psych.domain.model.SessionStatus;
import com.edutech.psych.domain.model.SessionType;
import com.edutech.psych.infrastructure.security.JwtTokenValidator;
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

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for psych-svc.
 *
 * Strategy:
 *   - PostgreSQLContainer runs the full Flyway migration chain (V1–V5).
 *   - KafkaContainer satisfies spring-kafka auto-configuration.
 *   - JwtTokenValidator is @MockBean — its constructor reads an RSA key from disk
 *     which is not available in CI; the mock short-circuits that.
 *   - Redis and pgvector extensions are not exercised in these tests.
 *   - Tests exercise: PsychProfileService (create/activate) and SessionService
 *     (startSession → completeSession with trait dimension update) end-to-end.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@Transactional
class PsychAssessmentIT {

    // ---------------------------------------------------------------------------
    // Infrastructure containers
    // ---------------------------------------------------------------------------

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("psych_db_test")
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
    // Beans under test
    // ---------------------------------------------------------------------------

    @Autowired
    PsychProfileService psychProfileService;

    @Autowired
    SessionService sessionService;

    /**
     * JwtTokenValidator's constructor calls KeyFactory.generatePublic() using a
     * file path from properties.  Mock it so the application context loads without
     * a real RSA key on the test classpath.
     */
    @MockBean
    JwtTokenValidator jwtTokenValidator;

    // ---------------------------------------------------------------------------
    // Test fixtures
    // ---------------------------------------------------------------------------

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID CENTER_ID  = UUID.randomUUID();
    private static final UUID BATCH_ID   = UUID.randomUUID();

    /**
     * A SUPER_ADMIN principal bypasses all centerId and studentId ownership checks
     * across both PsychProfileService and SessionService, keeping tests concise.
     */
    private AuthPrincipal superAdmin() {
        return new AuthPrincipal(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "admin@test.com",
                Role.SUPER_ADMIN,
                null,
                "fp-admin"
        );
    }

    /**
     * A STUDENT principal whose userId matches STUDENT_ID — required by
     * SessionService.startSession and completeSession ownership checks.
     */
    private AuthPrincipal studentPrincipal() {
        return new AuthPrincipal(STUDENT_ID, "student@test.com", Role.STUDENT, CENTER_ID, "fp-student");
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("createPsychProfile_forStudent: profile is persisted with ACTIVE status and zeroed traits")
    void createPsychProfile_forStudent() {
        CreatePsychProfileRequest request = new CreatePsychProfileRequest(
                STUDENT_ID, CENTER_ID, BATCH_ID
        );

        var response = psychProfileService.createProfile(request, superAdmin());

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.studentId()).isEqualTo(STUDENT_ID);
        assertThat(response.centerId()).isEqualTo(CENTER_ID);
        assertThat(response.batchId()).isEqualTo(BATCH_ID);
        // PsychProfileService.createProfile calls profile.activate() immediately
        assertThat(response.status()).isEqualTo(ProfileStatus.ACTIVE);
        // Initial trait scores are zero
        assertThat(response.openness()).isEqualTo(0.0);
        assertThat(response.conscientiousness()).isEqualTo(0.0);
        assertThat(response.extraversion()).isEqualTo(0.0);
        assertThat(response.agreeableness()).isEqualTo(0.0);
        assertThat(response.neuroticism()).isEqualTo(0.0);
        assertThat(response.riasecCode()).isNull();
        assertThat(response.createdAt()).isNotNull();

        // Verify retrieval by profile id
        var retrieved = psychProfileService.getProfile(response.id(), superAdmin());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().id()).isEqualTo(response.id());
        assertThat(retrieved.get().status()).isEqualTo(ProfileStatus.ACTIVE);
    }

    @Test
    @DisplayName("submitTraitScores_computesDimensions: completing a session updates Big-Five traits on the profile")
    void submitTraitScores_computesDimensions() {
        // Create and activate a profile
        var profile = psychProfileService.createProfile(
                new CreatePsychProfileRequest(STUDENT_ID, CENTER_ID, BATCH_ID),
                superAdmin()
        );

        // Start a session as the student
        StartSessionRequest startReq = new StartSessionRequest(
                SessionType.INITIAL,
                Instant.now().plusSeconds(600)
        );
        var sessionResponse = sessionService.startSession(profile.id(), startReq, studentPrincipal());

        assertThat(sessionResponse).isNotNull();
        assertThat(sessionResponse.id()).isNotNull();
        assertThat(sessionResponse.profileId()).isEqualTo(profile.id());
        assertThat(sessionResponse.studentId()).isEqualTo(STUDENT_ID);
        assertThat(sessionResponse.sessionType()).isEqualTo(SessionType.INITIAL);
        assertThat(sessionResponse.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(sessionResponse.startedAt()).isNotNull();

        // Complete the session, supplying Big-Five scores
        CompleteSessionRequest completeReq = new CompleteSessionRequest(
                0.82,   // openness
                0.75,   // conscientiousness
                0.60,   // extraversion
                0.70,   // agreeableness
                0.25,   // neuroticism
                "RIA",  // riasecCode
                "Initial assessment notes — student shows strong openness."
        );
        var completed = sessionService.completeSession(sessionResponse.id(), completeReq, studentPrincipal());

        assertThat(completed.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completed.completedAt()).isNotNull();
        assertThat(completed.notes()).isEqualTo(
                "Initial assessment notes — student shows strong openness.");

        // Verify the profile's trait dimensions were updated
        var updatedProfile = psychProfileService.getProfile(profile.id(), superAdmin());
        assertThat(updatedProfile).isPresent();
        assertThat(updatedProfile.get().openness()).isEqualTo(0.82);
        assertThat(updatedProfile.get().conscientiousness()).isEqualTo(0.75);
        assertThat(updatedProfile.get().extraversion()).isEqualTo(0.60);
        assertThat(updatedProfile.get().agreeableness()).isEqualTo(0.70);
        assertThat(updatedProfile.get().neuroticism()).isEqualTo(0.25);
        assertThat(updatedProfile.get().riasecCode()).isEqualTo("RIA");
    }

    @Test
    @DisplayName("getSessionHistory_returnsCompletedSessions: completed sessions are visible in session list")
    void getSessionHistory_returnsCompletedSessions() {
        var profile = psychProfileService.createProfile(
                new CreatePsychProfileRequest(STUDENT_ID, CENTER_ID, BATCH_ID),
                superAdmin()
        );

        // Session 1 — start and complete
        var s1 = sessionService.startSession(profile.id(),
                new StartSessionRequest(SessionType.INITIAL, Instant.now().plusSeconds(60)),
                studentPrincipal());
        sessionService.completeSession(s1.id(),
                new CompleteSessionRequest(0.50, 0.55, 0.60, 0.65, 0.35, "RI", "First session notes"),
                studentPrincipal());

        // Session 2 — start and complete
        var s2 = sessionService.startSession(profile.id(),
                new StartSessionRequest(SessionType.PERIODIC, Instant.now().plusSeconds(120)),
                studentPrincipal());
        sessionService.completeSession(s2.id(),
                new CompleteSessionRequest(0.60, 0.65, 0.55, 0.70, 0.30, "RIA", "Second session notes"),
                studentPrincipal());

        // List all sessions for the profile
        var sessions = sessionService.listSessions(profile.id(), studentPrincipal());

        assertThat(sessions).hasSize(2);

        var completedStatuses = sessions.stream()
                .map(sr -> sr.status())
                .toList();
        assertThat(completedStatuses).containsOnly(SessionStatus.COMPLETED);

        var sessionTypes = sessions.stream()
                .map(sr -> sr.sessionType())
                .toList();
        assertThat(sessionTypes).containsExactlyInAnyOrder(SessionType.INITIAL, SessionType.PERIODIC);

        // Each completed session must have a completedAt timestamp
        sessions.forEach(s -> assertThat(s.completedAt()).isNotNull());
    }

    @Test
    @DisplayName("listByStudentId_returnsProfile: the profile for a student is retrievable by studentId")
    void listByStudentId_returnsProfile() {
        // V6: only one active profile per student is allowed
        psychProfileService.createProfile(
                new CreatePsychProfileRequest(STUDENT_ID, CENTER_ID, BATCH_ID),
                superAdmin()
        );

        var profiles = psychProfileService.listByStudentId(STUDENT_ID, superAdmin());

        assertThat(profiles).hasSize(1);
        assertThat(profiles.get(0).studentId()).isEqualTo(STUDENT_ID);
        assertThat(profiles.get(0).status()).isEqualTo(ProfileStatus.ACTIVE);
    }

    @Test
    @DisplayName("createProfile_selfService: null centerId and batchId are accepted — self-service creation")
    void createProfile_selfService_nullCenterAndBatch() {
        UUID selfId = UUID.randomUUID();

        // A self-service request carries no centerId or batchId
        var response = psychProfileService.createProfile(
                new CreatePsychProfileRequest(selfId, null, null),
                superAdmin()
        );

        assertThat(response).isNotNull();
        assertThat(response.studentId()).isEqualTo(selfId);
        assertThat(response.centerId()).isNull();
        assertThat(response.batchId()).isNull();
        assertThat(response.status()).isEqualTo(ProfileStatus.ACTIVE);
        assertThat(response.openness()).isEqualTo(0.0);
    }
}
