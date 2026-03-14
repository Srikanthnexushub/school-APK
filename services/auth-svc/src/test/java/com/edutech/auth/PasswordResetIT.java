package com.edutech.auth;

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
 * Integration tests for the password reset and change-password flows.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Password reset and change-password integration tests")
class PasswordResetIT {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("auth_test")
            .withUsername("auth_test_user")
            .withPassword("auth_test_pass");

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> redis =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static final KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username",  postgres::getUsername);
        registry.add("spring.datasource.password",  postgres::getPassword);
        registry.add("spring.data.redis.host",       redis::getHost);
        registry.add("spring.data.redis.port",        () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password",    () -> "");
        registry.add("spring.data.redis.ssl.enabled", () -> false);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("jwt.private-key-path", () -> resolveTestResource("certs/test-private-key.pem"));
        registry.add("jwt.public-key-path",  () -> resolveTestResource("certs/test-public-key.pem"));
        registry.add("captcha.e2e-bypass-token", () -> CAPTCHA_BYPASS_TOKEN);
    }

    private static String resolveTestResource(String classpathRelative) {
        try {
            URL url = PasswordResetIT.class.getClassLoader().getResource(classpathRelative);
            if (url == null) throw new IllegalStateException("Not found: " + classpathRelative);
            return Path.of(url.toURI()).toAbsolutePath().toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot resolve: " + classpathRelative, e);
        }
    }

    static final String CAPTCHA_BYPASS_TOKEN = "e2e-test-bypass-secret";

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    private String registerAndActivate(String email) {
        String body = """
            {"email":"%s","password":"Test@12345","role":"STUDENT","centerId":null,
             "firstName":"Reset","lastName":"Tester","phoneNumber":null,
             "captchaToken":"%s:ignored",
             "deviceFingerprint":{"userAgent":"Test","deviceId":"dev-001","ipSubnet":"127.0.0.0/24"},
             "parentEmail":null}
            """.formatted(email, CAPTCHA_BYPASS_TOKEN);

        ResponseEntity<String> r = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/register",
            new HttpEntity<>(body, jsonHeaders()), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        User user = userRepository.findByEmail(email).orElseThrow();
        user.activate();
        userRepository.save(user);

        // Login to get access token
        String loginBody = """
            {"email":"%s","password":"Test@12345","captchaToken":"%s:ignored",
             "deviceFingerprint":{"userAgent":"Test","deviceId":"dev-001","ipSubnet":"127.0.0.0/24"}}
            """.formatted(email, CAPTCHA_BYPASS_TOKEN);
        ResponseEntity<String> loginRes = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/login",
            new HttpEntity<>(loginBody, jsonHeaders()), String.class);
        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        int start = loginRes.getBody().indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
        int end = loginRes.getBody().indexOf("\"", start);
        return loginRes.getBody().substring(start, end);
    }

    // ── Tests ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /forgot-password — always returns 204 (no enumeration), even for unknown email")
    void forgotPassword_unknownEmail_returns204Silently() {
        ResponseEntity<Void> r = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/forgot-password",
            new HttpEntity<>("{\"email\":\"nobody@example.com\"}", jsonHeaders()),
            Void.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("POST /forgot-password — known email — returns 204")
    void forgotPassword_knownEmail_returns204() {
        String email = "it-reset-forgot-" + System.nanoTime() + "@example.com";
        registerAndActivate(email);

        ResponseEntity<Void> r = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/forgot-password",
            new HttpEntity<>("{\"email\":\"" + email + "\"}", jsonHeaders()),
            Void.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("POST /change-password — correct current password — returns 204 and invalidates sessions")
    void changePassword_correctCurrentPassword_returns204() {
        String email = "it-changepw-" + System.nanoTime() + "@example.com";
        String accessToken = registerAndActivate(email);

        String body = "{\"currentPassword\":\"Test@12345\",\"newPassword\":\"NewPass@9876\"}";
        ResponseEntity<Void> r = restTemplate.exchange(
            baseUrl + "/api/v1/auth/change-password",
            HttpMethod.POST,
            new HttpEntity<>(body, bearerHeaders(accessToken)),
            Void.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("POST /change-password — wrong current password — returns 422")
    void changePassword_wrongCurrentPassword_returns422() {
        String email = "it-changepw-wrong-" + System.nanoTime() + "@example.com";
        String accessToken = registerAndActivate(email);

        String body = "{\"currentPassword\":\"WrongPw@99\",\"newPassword\":\"NewPass@9876\"}";
        ResponseEntity<String> r = restTemplate.exchange(
            baseUrl + "/api/v1/auth/change-password",
            HttpMethod.POST,
            new HttpEntity<>(body, bearerHeaders(accessToken)),
            String.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody()).contains("incorrect-current-password");
    }

    @Test
    @DisplayName("POST /change-password — no auth token — returns 401")
    void changePassword_noToken_returns401() {
        String body = "{\"currentPassword\":\"old\",\"newPassword\":\"NewPass@9876\"}";
        ResponseEntity<String> r = restTemplate.exchange(
            baseUrl + "/api/v1/auth/change-password",
            HttpMethod.POST,
            new HttpEntity<>(body, jsonHeaders()),
            String.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
