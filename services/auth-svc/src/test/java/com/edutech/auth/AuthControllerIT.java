// src/test/java/com/edutech/auth/AuthControllerIT.java
package com.edutech.auth;

import com.edutech.auth.domain.model.Role;
import com.edutech.auth.domain.model.User;
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
import org.springframework.http.HttpMethod;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@code /api/v1/auth} endpoints.
 *
 * <p>Uses real PostgreSQL (Flyway-migrated), real Redis, and real Kafka via
 * Testcontainers. The Spring context starts once for the whole class (shared
 * containers) to keep the suite fast.
 *
 * <p>Captcha is bypassed via the {@code captcha.e2e-bypass-token} mechanism —
 * the request sends {@code "e2e-test-bypass-secret:ignored"} as the captchaToken
 * field, which {@link com.edutech.auth.infrastructure.external.LocalCaptchaVerifierAdapter}
 * accepts without any Redis lookup.
 *
 * <p>Login tests that require an ACTIVE user activate the user directly through
 * the domain port ({@code user.activate(); userRepository.save(user)}) to avoid
 * the OTP round-trip.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AuthController integration tests")
class AuthControllerIT {

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

    /** Kafka for audit events (published fire-and-forget — tests do not consume). */
    @Container
    static final KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    // ── Dynamic property wiring ──────────────────────────────────────────────

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username",  postgres::getUsername);
        registry.add("spring.datasource.password",  postgres::getPassword);

        // Redis
        registry.add("spring.data.redis.host",        redis::getHost);
        registry.add("spring.data.redis.port",         () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password",     () -> "");
        registry.add("spring.data.redis.ssl.enabled",  () -> false);

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // JWT RSA key files — resolved from the test-classes directory at runtime
        registry.add("jwt.private-key-path", () -> resolveTestResource("certs/test-private-key.pem"));
        registry.add("jwt.public-key-path",  () -> resolveTestResource("certs/test-public-key.pem"));

        // Captcha bypass — must match the value in application-test.yml
        registry.add("captcha.e2e-bypass-token", () -> CAPTCHA_BYPASS_TOKEN);
    }

    /**
     * Resolves a classpath resource to an absolute file-system path so that
     * JwtTokenProvider / JwtTokenValidator can read the PEM files.
     */
    private static String resolveTestResource(String classpathRelative) {
        try {
            URL url = AuthControllerIT.class.getClassLoader().getResource(classpathRelative);
            if (url == null) {
                throw new IllegalStateException(
                    "Test resource not found on classpath: " + classpathRelative);
            }
            return Path.of(url.toURI()).toAbsolutePath().toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot resolve test resource: " + classpathRelative, e);
        }
    }

    // ── Test infrastructure ──────────────────────────────────────────────────

    /**
     * The captcha bypass token configured in {@code application-test.yml}.
     * A request captchaToken of {@code "{BYPASS_TOKEN}:ignored"} passes verification
     * without any Redis lookup.
     */
    static final String CAPTCHA_BYPASS_TOKEN = "e2e-test-bypass-secret";

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Builds a minimal valid registration JSON payload.
     *
     * @param email unique email address for this test invocation
     * @return JSON string ready to POST
     */
    private String buildRegisterJson(String email) {
        return """
            {
              "email": "%s",
              "password": "Test@12345",
              "role": "STUDENT",
              "centerId": null,
              "firstName": "Alice",
              "lastName": "Tester",
              "phoneNumber": null,
              "captchaToken": "%s:ignored",
              "deviceFingerprint": {
                "userAgent": "TestAgent/1.0",
                "deviceId": "test-device-ctrl-001",
                "ipSubnet": "127.0.0.0/24"
              },
              "parentEmail": null
            }
            """.formatted(email, CAPTCHA_BYPASS_TOKEN);
    }

    /**
     * Builds a minimal valid login JSON payload.
     *
     * @param email    the registered user's email
     * @param password the user's password (correct or intentionally wrong)
     * @return JSON string ready to POST
     */
    private String buildLoginJson(String email, String password) {
        return """
            {
              "email": "%s",
              "password": "%s",
              "captchaToken": "%s:ignored",
              "deviceFingerprint": {
                "userAgent": "TestAgent/1.0",
                "deviceId": "test-device-ctrl-001",
                "ipSubnet": "127.0.0.0/24"
              }
            }
            """.formatted(email, password, CAPTCHA_BYPASS_TOKEN);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders bearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    /**
     * Registers a new user and returns the raw response body (contains the TokenPair).
     * Asserts that registration itself succeeded with 201 before returning.
     */
    private String registerAndGetBody(String email) {
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/register",
            new HttpEntity<>(buildRegisterJson(email), jsonHeaders()),
            String.class
        );
        assertThat(response.getStatusCode())
            .as("Registration prerequisite must succeed with HTTP 201")
            .isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    /**
     * Extracts the accessToken JSON string value from a TokenPair response body.
     * Uses simple string extraction to avoid pulling in a JSON library dependency.
     */
    private String extractAccessToken(String responseBody) {
        // ResponseBody contains: {"accessToken":"<token>","refreshToken":...}
        int start = responseBody.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
        int end   = responseBody.indexOf("\"", start);
        String token = responseBody.substring(start, end);
        assertThat(token).as("Extracted accessToken must not be blank").isNotBlank();
        return token;
    }

    /**
     * Activates a PENDING_VERIFICATION user directly through the domain repository,
     * bypassing the OTP flow. Required before any login test that expects HTTP 200.
     */
    private void activateUser(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("User not found: " + email));
        user.activate();
        userRepository.save(user);
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/auth/register — happy path returns 201 with accessToken and refreshToken")
    void register_happyPath_returns201WithTokenPair() {
        String email = "it-ctrl-register-" + System.nanoTime() + "@example.com";

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/register",
            new HttpEntity<>(buildRegisterJson(email), jsonHeaders()),
            String.class
        );

        assertThat(response.getStatusCode())
            .as("Registration must return HTTP 201 Created")
            .isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody())
            .as("Response body must contain accessToken")
            .contains("accessToken");

        assertThat(response.getBody())
            .as("Response body must contain refreshToken")
            .contains("refreshToken");

        assertThat(response.getBody())
            .as("Response body must contain tokenType Bearer")
            .contains("Bearer");

        // Verify user was persisted in the database
        assertThat(userRepository.findByEmail(email))
            .as("Registered user must be findable in the database")
            .isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — happy path returns 200 with accessToken (after OTP activation)")
    void login_happyPath_returns200WithTokenPair() {
        String email = "it-ctrl-login-" + System.nanoTime() + "@example.com";

        // Step 1: register the user
        registerAndGetBody(email);

        // Step 2: activate the user directly (bypasses OTP flow for test speed)
        activateUser(email);

        // Step 3: login
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/login",
            new HttpEntity<>(buildLoginJson(email, "Test@12345"), jsonHeaders()),
            String.class
        );

        assertThat(response.getStatusCode())
            .as("Login with correct credentials must return HTTP 200 OK")
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as("Login response body must contain accessToken")
            .contains("accessToken");

        assertThat(response.getBody())
            .as("Login response body must contain refreshToken")
            .contains("refreshToken");
    }

    @Test
    @DisplayName("GET /api/v1/auth/me — returns 200 with user info when authenticated")
    void me_withValidToken_returns200WithUserInfo() {
        String email = "it-ctrl-me-" + System.nanoTime() + "@example.com";

        // Register the user — the returned access token is valid for /me even in
        // PENDING_VERIFICATION status (the JWT is issued at registration time and
        // the /me endpoint only validates the JWT, not the account status).
        String registerBody = registerAndGetBody(email);
        String accessToken  = extractAccessToken(registerBody);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/api/v1/auth/me",
            HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(accessToken)),
            String.class
        );

        assertThat(response.getStatusCode())
            .as("GET /me with a valid Bearer token must return HTTP 200 OK")
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as("Response body must contain the registered email")
            .contains(email);

        assertThat(response.getBody())
            .as("Response body must contain the user's first name")
            .contains("Alice");

        assertThat(response.getBody())
            .as("Response body must contain the user's last name")
            .contains("Tester");

        assertThat(response.getBody())
            .as("Response body must contain the user's role")
            .contains("STUDENT");
    }

    @Test
    @DisplayName("GET /api/v1/auth/me — returns 401 when no Bearer token is provided")
    void me_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/api/v1/auth/me",
            HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            String.class
        );

        assertThat(response.getStatusCode())
            .as("GET /me without authentication must return HTTP 401 Unauthorized")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — wrong password returns 401 Unauthorized")
    void login_wrongPassword_returns401() {
        String email = "it-ctrl-badpw-" + System.nanoTime() + "@example.com";

        // Register and activate so the user exists and is ACTIVE
        registerAndGetBody(email);
        activateUser(email);

        // Attempt login with an incorrect password
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/login",
            new HttpEntity<>(buildLoginJson(email, "WrongPassword!99"), jsonHeaders()),
            String.class
        );

        assertThat(response.getStatusCode())
            .as("Login with wrong password must return HTTP 401 Unauthorized")
            .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(response.getBody())
            .as("Error response body must contain the invalid-credentials problem type")
            .contains("invalid-credentials");
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — unverified user returns 403 Forbidden")
    void login_pendingVerificationUser_returns403() {
        String email = "it-ctrl-unverified-" + System.nanoTime() + "@example.com";

        // Register but do NOT activate — user remains PENDING_VERIFICATION
        registerAndGetBody(email);

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/login",
            new HttpEntity<>(buildLoginJson(email, "Test@12345"), jsonHeaders()),
            String.class
        );

        assertThat(response.getStatusCode())
            .as("Login attempt by an unverified user must return HTTP 403 Forbidden")
            .isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(response.getBody())
            .as("Error response body must contain the account-not-verified problem type")
            .contains("account-not-verified");
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — duplicate email returns 409 Conflict")
    void register_duplicateEmail_returns409() {
        String email = "it-ctrl-dup-" + System.nanoTime() + "@example.com";

        // First registration must succeed
        registerAndGetBody(email);

        // Second registration with the same email must be rejected
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/register",
            new HttpEntity<>(buildRegisterJson(email), jsonHeaders()),
            String.class
        );

        assertThat(response.getStatusCode())
            .as("Registering the same email a second time must return HTTP 409 Conflict")
            .isEqualTo(HttpStatus.CONFLICT);

        assertThat(response.getBody())
            .as("409 response body must describe the email-already-exists problem")
            .contains("email-already-exists");
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — non-existent user returns 401 Unauthorized")
    void login_unknownEmail_returns401() {
        String email = "it-ctrl-ghost-" + System.nanoTime() + "@example.com";

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/login",
            new HttpEntity<>(buildLoginJson(email, "Test@12345"), jsonHeaders()),
            String.class
        );

        assertThat(response.getStatusCode())
            .as("Login for a non-existent user must return HTTP 401 Unauthorized (no user enumeration)")
            .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(response.getBody())
            .as("Error response body must contain the invalid-credentials problem type")
            .contains("invalid-credentials");
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — user is stored with PENDING_VERIFICATION status")
    void register_newUser_hasPendingVerificationStatus() {
        String email = "it-ctrl-status-" + System.nanoTime() + "@example.com";

        registerAndGetBody(email);

        User persisted = userRepository.findByEmail(email)
            .orElseThrow(() -> new AssertionError("User must exist after registration"));

        assertThat(persisted.isPendingVerification())
            .as("Newly registered user must start in PENDING_VERIFICATION status")
            .isTrue();

        assertThat(persisted.isEmailVerified())
            .as("Email must not be marked verified until OTP is confirmed")
            .isFalse();

        assertThat(persisted.getRole())
            .as("User must be saved with the requested role")
            .isEqualTo(Role.STUDENT);
    }
}
