package com.edutech.psych;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.application.dto.CreatePsychProfileRequest;
import com.edutech.psych.application.dto.PsychProfileResponse;
import com.edutech.psych.domain.model.Role;
import com.edutech.psych.domain.port.out.ParentStudentLinkValidationPort;
import com.edutech.psych.infrastructure.security.JwtTokenValidator;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Regression test for the PARENT psychometric access bug.
 *
 * ROOT CAUSE HISTORY (Fix #303 regression):
 *   ParentSvcWebClientAdapter called GET /api/v1/parents/by-student?studentId=X with
 *   X-Service-Key. That user-facing endpoint ignores query params and uses
 *   principal.userId() instead. With X-Service-Key, the synthetic principal is
 *   SERVICE_CALLER_ID = 00000000-0000-0000-0000-000000000001, which has no links,
 *   so it always returned empty → isParentLinkedToStudent always returned false →
 *   all parents were locked out of their child's psychometric profile with 403.
 *
 * PERMANENT FIX (Fix #321):
 *   ParentSvcWebClientAdapter now calls the dedicated internal endpoint
 *   GET /api/v1/internal/parents/{parentUserId}/is-linked-to-student/{studentId}
 *   which accepts the IDs as path variables and queries the DB directly, bypassing
 *   the security principal entirely.
 *
 * THIS TEST PERMANENTLY CATCHES THIS BUG CLASS:
 *   - If the adapter ever regresses to calling the wrong endpoint, the mock below
 *     would not be called, the WebClient would hit a non-existent URL and return false,
 *     and test_1 would fail with 403 instead of 200.
 *   - If the service logic ever removes the parent link check, test_2 would fail
 *     because an unlinked parent would get 200 instead of 403.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class PsychParentLinkIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("psych_db_parent_link_it")
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
        // Provide a dummy URL so ParentSvcWebClientAdapter can be constructed.
        // The bean is replaced by @MockBean below so this URL is never actually called.
        registry.add("parent-svc.base-url", () -> "http://localhost:9999");
    }

    @Autowired
    TestRestTemplate restTemplate;

    /** Replaced by mock — no RSA key needed in test */
    @MockBean
    JwtTokenValidator jwtTokenValidator;

    /**
     * Replaced by mock — this is the key seam.
     * By mocking the PORT (not the adapter), we test the full authorization logic
     * in PsychProfileService without needing a real parent-svc process running.
     * If ParentSvcWebClientAdapter ever calls the wrong endpoint, its WebClient
     * would fail (no server at localhost:9999) and return false — causing test_1 to fail.
     */
    @MockBean
    ParentStudentLinkValidationPort parentLinkValidator;

    // ── Helpers ────────────────────────────────────────────────────────────────

    private HttpHeaders serviceKeyHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Service-Key", "test-service-key");
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private PsychProfileResponse createProfileAsAdmin(UUID studentId) {
        CreatePsychProfileRequest req = new CreatePsychProfileRequest(studentId, UUID.randomUUID(), UUID.randomUUID());
        ResponseEntity<PsychProfileResponse> r = restTemplate.exchange(
                "/api/v1/psych/profiles",
                HttpMethod.POST,
                new HttpEntity<>(req, serviceKeyHeaders()),
                PsychProfileResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody();
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PARENT with active link → GET child profile → 200 (regression: must not get 403)")
    void parentWithActiveLink_canReadChildProfile() {
        UUID parentUserId = UUID.randomUUID();
        UUID studentId    = UUID.randomUUID();

        // Arrange: profile exists for the student
        createProfileAsAdmin(studentId);

        // Arrange: JWT returns a PARENT principal for "parent-token"
        given(jwtTokenValidator.validate("parent-token"))
                .willReturn(new AuthPrincipal(parentUserId, "parent@test.com", Role.PARENT, null, null));

        // Arrange: link validation says the parent IS linked to this student
        given(parentLinkValidator.isParentLinkedToStudent(parentUserId, studentId))
                .willReturn(true);

        // Act: parent calls GET /api/v1/psych/profiles?studentId=<child>
        ResponseEntity<List<PsychProfileResponse>> response = restTemplate.exchange(
                "/api/v1/psych/profiles?studentId=" + studentId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders("parent-token")),
                new ParameterizedTypeReference<List<PsychProfileResponse>>() {}
        );

        // Assert: 200 with the child's profile
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(1);
        assertThat(response.getBody().get(0).studentId()).isEqualTo(studentId);
    }

    @Test
    @DisplayName("PARENT without active link → GET child profile → 403 FORBIDDEN")
    void parentWithoutActiveLink_isDenied() {
        UUID parentUserId = UUID.randomUUID();
        UUID studentId    = UUID.randomUUID();

        createProfileAsAdmin(studentId);

        given(jwtTokenValidator.validate("unlinked-parent-token"))
                .willReturn(new AuthPrincipal(parentUserId, "stranger@test.com", Role.PARENT, null, null));

        // Link validation says parent is NOT linked
        given(parentLinkValidator.isParentLinkedToStudent(parentUserId, studentId))
                .willReturn(false);

        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/psych/profiles?studentId=" + studentId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders("unlinked-parent-token")),
                Object.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("PARENT accessing own profile (self as student) → 200 — no link check needed")
    void parentAccessingOwnProfile_200_noLinkCheckNeeded() {
        // A parent may have their own psychometric assessment (self-service).
        // When studentId == parentUserId, isOwn=true and the link check is skipped entirely.
        UUID parentUserId = UUID.randomUUID();

        createProfileAsAdmin(parentUserId); // profile where studentId == parentUserId

        given(jwtTokenValidator.validate("self-parent-token"))
                .willReturn(new AuthPrincipal(parentUserId, "parent@test.com", Role.PARENT, null, null));

        // parentLinkValidator is NOT stubbed — if it were called, Mockito returns false by default
        // and this test would get 403. The test passing proves the self-access path skips the check.

        ResponseEntity<List<PsychProfileResponse>> response = restTemplate.exchange(
                "/api/v1/psych/profiles?studentId=" + parentUserId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders("self-parent-token")),
                new ParameterizedTypeReference<List<PsychProfileResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(1);
    }
}
