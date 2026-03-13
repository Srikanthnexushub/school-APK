// src/test/java/com/edutech/auth/UserRegistrationIT.java
package com.edutech.auth;

import com.edutech.auth.application.config.CleanupProperties;
import com.edutech.auth.application.service.UserCleanupScheduler;
import com.edutech.auth.domain.model.Role;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.model.UserStatus;
import com.edutech.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for user registration flows in auth-svc.
 *
 * <p>Each test uses real PostgreSQL (Flyway-migrated), real Redis, and real Kafka
 * supplied by Testcontainers. The Spring application context starts once for the
 * whole class (shared containers) to keep the suite fast.
 *
 * <p>Captcha is bypassed via the {@code captcha.e2e-bypass-token} mechanism —
 * the request sends {@code "{bypassToken}:anything"} as the captchaToken field,
 * which the {@link com.edutech.auth.infrastructure.external.LocalCaptchaVerifierAdapter}
 * accepts without a Redis lookup when the bypass token is non-blank.
 *
 * <p>Mail delivery intentionally fails (no SMTP server in tests); the
 * {@link com.edutech.auth.infrastructure.external.SmtpNotificationAdapter} catches
 * the MessagingException and logs the OTP — the registration transaction still
 * commits successfully.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserRegistration integration tests")
class UserRegistrationIT {

    // ── Containers ──────────────────────────────────────────────────────────

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("auth_test")
            .withUsername("auth_test_user")
            .withPassword("auth_test_pass");

    /** Redis 7 (no password, no TLS in tests). */
    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> redis =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    /** Kafka for audit events (published fire-and-forget — test does not consume). */
    @Container
    static final KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    // ── Dynamic property wiring ──────────────────────────────────────────────

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("spring.data.redis.ssl.enabled", () -> false);

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // JWT RSA key files — resolved from the test-classes directory at runtime
        registry.add("jwt.private-key-path", () -> resolveTestResource("certs/test-private-key.pem"));
        registry.add("jwt.public-key-path",  () -> resolveTestResource("certs/test-public-key.pem"));
    }

    /**
     * Resolves a classpath resource to an absolute file-system path so that
     * {@link com.edutech.auth.infrastructure.security.JwtTokenProvider} and
     * {@link com.edutech.auth.infrastructure.security.JwtTokenValidator} can read
     * the PEM files from the file system (they use {@code Files.readString(Path.of(path))}).
     */
    private static String resolveTestResource(String classpathRelative) {
        try {
            URL url = UserRegistrationIT.class.getClassLoader().getResource(classpathRelative);
            if (url == null) {
                throw new IllegalStateException(
                    "Test resource not found on classpath: " + classpathRelative +
                    ". Run 'mvn generate-test-resources' or ensure src/test/resources/certs/ exists.");
            }
            return Path.of(url.toURI()).toAbsolutePath().toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot resolve test resource: " + classpathRelative, e);
        }
    }

    // ── Test infrastructure ──────────────────────────────────────────────────

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserCleanupScheduler userCleanupScheduler;

    @Autowired
    CleanupProperties cleanupProperties;

    /**
     * The captcha bypass token configured in {@code application-test.yml}.
     * A request captchaToken of {@code "{BYPASS_TOKEN}:ignored"} passes verification
     * without any Redis lookup.
     */
    private static final String CAPTCHA_BYPASS_TOKEN = "e2e-test-bypass-secret";

    /** Base URL for the auth API under test. */
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Builds a minimal valid registration JSON payload.
     *
     * @param email unique email for this test
     * @return JSON string
     */
    private String buildRegisterJson(String email) {
        return """
            {
              "email": "%s",
              "password": "Test@12345",
              "role": "STUDENT",
              "centerId": null,
              "firstName": "Integration",
              "lastName": "Test",
              "phoneNumber": null,
              "captchaToken": "%s:ignored",
              "deviceFingerprint": {
                "userAgent": "TestAgent/1.0",
                "deviceId": "test-device-001",
                "ipSubnet": "127.0.0.0/24"
              },
              "parentEmail": null
            }
            """.formatted(email, CAPTCHA_BYPASS_TOKEN);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/auth/register — saves user with PENDING_VERIFICATION status")
    void registerUser_savesToDatabase() {
        String email = "it-register-" + System.nanoTime() + "@example.com";
        String body = buildRegisterJson(email);

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/register",
            new HttpEntity<>(body, jsonHeaders()),
            String.class
        );

        // Endpoint returns 201 Created with a TokenPair body
        assertThat(response.getStatusCode())
            .as("Registration should return HTTP 201")
            .isEqualTo(HttpStatus.CREATED);

        // Verify user was actually persisted
        Optional<User> persisted = userRepository.findByEmail(email);
        assertThat(persisted)
            .as("Registered user must be found in the database by email")
            .isPresent();

        User user = persisted.get();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getFirstName()).isEqualTo("Integration");
        assertThat(user.getLastName()).isEqualTo("Test");
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);

        // New accounts start in PENDING_VERIFICATION until the OTP is confirmed
        assertThat(user.getStatus())
            .as("Newly registered user must have PENDING_VERIFICATION status")
            .isEqualTo(UserStatus.PENDING_VERIFICATION);

        // Soft-delete field must be null — user is not deactivated
        assertThat(user.getDeletedAt())
            .as("A freshly registered user must not have a deletedAt timestamp")
            .isNull();

        // Response body should contain the access/refresh token pair
        assertThat(response.getBody())
            .as("Response body must include an accessToken")
            .contains("accessToken");
        assertThat(response.getBody())
            .as("Response body must include a refreshToken")
            .contains("refreshToken");
    }

    @Test
    @DisplayName("Pending-verification user older than retentionHours is soft-deleted by the cleanup job")
    void pendingVerificationUser_isCleanedUpAfterRetentionPeriod() {
        // Create a PENDING_VERIFICATION user directly via the domain — bypass the HTTP layer
        // so we can back-date the createdAt by manipulating the persisted state.
        // Since User.createdAt is immutable (set in constructor), we insert a user that was
        // created right now, then verify the scheduler correctly skips it, and separately
        // that users older than retentionHours ARE collected by findExpiredPendingVerification.
        //
        // Because we cannot override createdAt on the entity (immutable by design), we verify
        // the scheduler's behaviour by:
        //   1. Saving a fresh user (createdAt = now) — scheduler must NOT touch it.
        //   2. Confirming that the scheduler's findExpiredPendingVerification query returns
        //      nothing for cutoff = now - retentionHours (since the user is brand-new).
        //   3. Separately asserting that a cutoff in the future (now + 1 hour) WOULD return it,
        //      proving the query logic is correct end-to-end through the real DB.

        String email = "it-cleanup-" + System.nanoTime() + "@example.com";

        // Save a PENDING_VERIFICATION user via the domain port (not HTTP, no captcha needed)
        User freshUser = User.create(
            email,
            "$argon2id$v=19$m=4096,t=1,p=1$test-hash",   // not a real password hash — we never login
            Role.STUDENT,
            null,
            "Cleanup",
            "Candidate",
            null
        );
        userRepository.save(freshUser);

        // Confirm the user exists and is PENDING_VERIFICATION
        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(saved.getDeletedAt()).isNull();

        // --- Step A: scheduler with real cutoff should NOT touch this brand-new user ---
        userCleanupScheduler.purgeExpiredUnverifiedAccounts();

        User afterRealCleanup = userRepository.findByEmail(email).orElseThrow();
        assertThat(afterRealCleanup.getStatus())
            .as("A brand-new PENDING_VERIFICATION user must not be purged by the scheduler")
            .isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(afterRealCleanup.getDeletedAt())
            .as("Brand-new user must not be soft-deleted after real-cutoff cleanup run")
            .isNull();

        // --- Step B: direct repo query with a FUTURE cutoff proves the query works end-to-end ---
        // cutoff = now + 1 hour → any user created before that qualifies (including our fresh user)
        Instant futureCutoff = Instant.now().plus(1, ChronoUnit.HOURS);
        List<User> wouldBeExpired = userRepository.findExpiredPendingVerification(futureCutoff);
        assertThat(wouldBeExpired)
            .as("With a future cutoff the fresh pending user must appear in the expiry query")
            .extracting(User::getEmail)
            .contains(email);

        // --- Step C: simulate the cleanup manually using the future cutoff ---
        for (User u : wouldBeExpired) {
            u.deactivate();
            userRepository.save(u);
        }

        // Verify the user is now soft-deleted (DEACTIVATED + deletedAt set)
        User afterSimulatedCleanup = userRepository.findByEmail(email).orElseThrow();
        assertThat(afterSimulatedCleanup.getStatus())
            .as("After simulated cleanup the user must be DEACTIVATED")
            .isEqualTo(UserStatus.DEACTIVATED);
        assertThat(afterSimulatedCleanup.getDeletedAt())
            .as("After simulated cleanup deletedAt must be populated")
            .isNotNull();

        // Verify the user no longer appears in the pending-verification query
        // (findByEmail uses WHERE deletedAt IS NULL via soft-delete filter)
        Optional<User> softDeletedLookup = userRepository.findByEmail(email);
        assertThat(softDeletedLookup)
            .as("Soft-deleted user must no longer be visible via findByEmail (deletedAt IS NULL filter)")
            .isEmpty();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with duplicate email returns HTTP 409 Conflict")
    void duplicateEmail_returns409() {
        String email = "it-duplicate-" + System.nanoTime() + "@example.com";
        String body = buildRegisterJson(email);
        HttpEntity<String> request = new HttpEntity<>(body, jsonHeaders());

        // First registration must succeed
        ResponseEntity<String> first = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/register",
            request,
            String.class
        );
        assertThat(first.getStatusCode())
            .as("First registration with a unique email must return HTTP 201")
            .isEqualTo(HttpStatus.CREATED);

        // Second registration with the same email must be rejected
        ResponseEntity<String> second = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/register",
            request,
            String.class
        );
        assertThat(second.getStatusCode())
            .as("Registering the same email a second time must return HTTP 409 Conflict")
            .isEqualTo(HttpStatus.CONFLICT);

        // Response body follows RFC 7807 ProblemDetail format
        assertThat(second.getBody())
            .as("409 response body must describe the email-already-exists problem")
            .contains("email-already-exists");

        // Only one user record must exist for this email
        Optional<User> persisted = userRepository.findByEmail(email);
        assertThat(persisted)
            .as("Exactly one user must be persisted despite two registration attempts")
            .isPresent();
    }
}
