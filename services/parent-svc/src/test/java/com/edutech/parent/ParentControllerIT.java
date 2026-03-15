package com.edutech.parent;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.CreateParentProfileRequest;
import com.edutech.parent.application.dto.LinkStudentRequest;
import com.edutech.parent.application.dto.ParentProfileResponse;
import com.edutech.parent.application.dto.StudentLinkResponse;
import com.edutech.parent.application.dto.UpdateParentProfileRequest;
import com.edutech.parent.domain.model.LinkStatus;
import com.edutech.parent.domain.model.ParentStatus;
import com.edutech.parent.domain.model.Role;
import com.edutech.parent.infrastructure.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * HTTP-layer integration tests for parent-svc controllers.
 *
 * Strategy:
 *   - RANDOM_PORT boots the full servlet stack with real DB (PostgreSQLContainer).
 *   - KafkaContainer satisfies spring-kafka auto-configuration.
 *   - JwtTokenValidator is @MockBean; for each test we configure it to return a
 *     controlled AuthPrincipal so the JwtAuthenticationFilter populates the
 *     SecurityContext without touching a real RSA key.
 *   - Requests are made via TestRestTemplate with a stub "Bearer test-token" header.
 *   - Tests cover: create profile, get profile, update profile, link student, 404 errors.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class ParentControllerIT {

    // ---------------------------------------------------------------------------
    // Infrastructure containers — shared across all tests in this class
    // ---------------------------------------------------------------------------

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("parent_db_it")
                    .withUsername("parent_user")
                    .withPassword("parent_pass");

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
     * JwtTokenValidator reads an RSA public key from disk in @PostConstruct.
     * Replace with a mock so the application context can start cleanly, and so
     * tests can inject any principal they need via Mockito stubbing.
     */
    @MockBean
    JwtTokenValidator jwtTokenValidator;

    // ---------------------------------------------------------------------------
    // Test fixtures
    // ---------------------------------------------------------------------------

    /** A unique userId per test class run — prevents cross-test profile collisions. */
    private static final UUID OWNER_USER_ID = UUID.randomUUID();
    private static final UUID CENTER_ID     = UUID.randomUUID();
    private static final UUID STUDENT_ID    = UUID.randomUUID();

    private static final String FAKE_TOKEN  = "test-bearer-token";

    private AuthPrincipal ownerPrincipal() {
        return new AuthPrincipal(OWNER_USER_ID, "owner@test.com", Role.PARENT, CENTER_ID, "fp-test");
    }

    private AuthPrincipal superAdminPrincipal() {
        return new AuthPrincipal(UUID.randomUUID(), "admin@test.com", Role.SUPER_ADMIN, null, "fp-admin");
    }

    /** Build headers with a stub Bearer token. */
    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(FAKE_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @BeforeEach
    void stubJwtValidator() {
        // Default stub: any token returns the owner principal.
        // Individual tests may override this with Mockito.reset() + new stub.
        given(jwtTokenValidator.validate(anyString()))
                .willReturn(Optional.of(ownerPrincipal()));
    }

    // ---------------------------------------------------------------------------
    // POST /api/v1/parents — create profile
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/parents — 201 with full profile response body")
    void createParentProfile_returns201() {
        CreateParentProfileRequest req = new CreateParentProfileRequest(
                "Suresh Verma",
                "+919964636666",
                "suresh@test.com",
                "12 Main Street",
                "Bengaluru",
                "Karnataka",
                "560001",
                "PARENT",
                null
        );

        ResponseEntity<ParentProfileResponse> response = restTemplate.exchange(
                "/api/v1/parents",
                HttpMethod.POST,
                new HttpEntity<>(req, authHeaders()),
                ParentProfileResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ParentProfileResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.userId()).isEqualTo(OWNER_USER_ID);
        assertThat(body.name()).isEqualTo("Suresh Verma");
        assertThat(body.phone()).isEqualTo("+919964636666");
        assertThat(body.email()).isEqualTo("suresh@test.com");
        assertThat(body.city()).isEqualTo("Bengaluru");
        assertThat(body.state()).isEqualTo("Karnataka");
        assertThat(body.pincode()).isEqualTo("560001");
        assertThat(body.relationshipType()).isEqualTo("PARENT");
        assertThat(body.verified()).isFalse();
        assertThat(body.status()).isEqualTo(ParentStatus.ACTIVE);
        assertThat(body.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("POST /api/v1/parents — 422 when name is blank (validation error)")
    void createParentProfile_returns422_whenNameBlank() {
        // name is @NotBlank — send empty string to trigger validation failure
        CreateParentProfileRequest req = new CreateParentProfileRequest(
                "",           // blank name — must fail @NotBlank
                "+919000000099",
                null, null, null, null, null, null, null
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/parents",
                HttpMethod.POST,
                new HttpEntity<>(req, authHeaders()),
                Map.class
        );

        // GlobalExceptionHandler maps MethodArgumentNotValidException to 422
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("title")).isEqualTo("Validation Error");
    }

    @Test
    @DisplayName("GET /api/v1/parents/me — 200 returns own profile after creation")
    void getMyProfile_returns200_afterCreation() {
        // First, create a profile (different userId to avoid collision with other tests)
        UUID freshUserId = UUID.randomUUID();
        given(jwtTokenValidator.validate(anyString()))
                .willReturn(Optional.of(new AuthPrincipal(freshUserId, "fresh@test.com", Role.PARENT, CENTER_ID, "fp")));

        CreateParentProfileRequest createReq = new CreateParentProfileRequest(
                "Fresh User", "+919000000011", "fresh@test.com",
                null, "Mumbai", "Maharashtra", "400001", "MOTHER", null
        );
        restTemplate.exchange("/api/v1/parents", HttpMethod.POST,
                new HttpEntity<>(createReq, authHeaders()), ParentProfileResponse.class);

        // Now GET /me with the same principal
        ResponseEntity<ParentProfileResponse> response = restTemplate.exchange(
                "/api/v1/parents/me",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                ParentProfileResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ParentProfileResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.userId()).isEqualTo(freshUserId);
        assertThat(body.name()).isEqualTo("Fresh User");
        assertThat(body.city()).isEqualTo("Mumbai");
    }

    @Test
    @DisplayName("PUT /api/v1/parents/{id} — 200 returns updated profile")
    void updateParentProfile_returns200_withUpdatedFields() {
        // Create a profile first
        UUID freshUserId = UUID.randomUUID();
        AuthPrincipal freshPrincipal = new AuthPrincipal(freshUserId, "update@test.com", Role.PARENT, CENTER_ID, "fp");
        given(jwtTokenValidator.validate(anyString())).willReturn(Optional.of(freshPrincipal));

        CreateParentProfileRequest createReq = new CreateParentProfileRequest(
                "Before Update", "+919000000022", "before@test.com",
                null, "Delhi", "Delhi", "110001", "PARENT", null
        );
        ResponseEntity<ParentProfileResponse> created = restTemplate.exchange(
                "/api/v1/parents", HttpMethod.POST,
                new HttpEntity<>(createReq, authHeaders()), ParentProfileResponse.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID profileId = created.getBody().id();

        // Update the profile
        UpdateParentProfileRequest updateReq = new UpdateParentProfileRequest(
                "After Update", "+919000000023", "after@test.com",
                "456 New Street", "Chennai", "Tamil Nadu", "600001", "GUARDIAN", null, null
        );
        ResponseEntity<ParentProfileResponse> updated = restTemplate.exchange(
                "/api/v1/parents/" + profileId,
                HttpMethod.PUT,
                new HttpEntity<>(updateReq, authHeaders()),
                ParentProfileResponse.class
        );

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        ParentProfileResponse body = updated.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(profileId);
        assertThat(body.name()).isEqualTo("After Update");
        assertThat(body.phone()).isEqualTo("+919000000023");
        assertThat(body.email()).isEqualTo("after@test.com");
        assertThat(body.city()).isEqualTo("Chennai");
        assertThat(body.state()).isEqualTo("Tamil Nadu");
        assertThat(body.pincode()).isEqualTo("600001");
        assertThat(body.relationshipType()).isEqualTo("GUARDIAN");
    }

    @Test
    @DisplayName("GET /api/v1/parents/{id} — 404 when profile does not exist")
    void getProfile_returns404_whenNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/parents/" + nonExistentId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("title")).isEqualTo("Parent Profile Not Found");
    }

    // ---------------------------------------------------------------------------
    // POST /api/v1/parents/{profileId}/students — link student
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/parents/{profileId}/students — 201 with student link response")
    void linkStudent_returns201() {
        // Create a profile first
        UUID freshUserId = UUID.randomUUID();
        AuthPrincipal freshPrincipal = new AuthPrincipal(freshUserId, "link@test.com", Role.PARENT, CENTER_ID, "fp");
        given(jwtTokenValidator.validate(anyString())).willReturn(Optional.of(freshPrincipal));

        CreateParentProfileRequest createReq = new CreateParentProfileRequest(
                "Link Parent", "+919000000033", "link@test.com",
                null, null, null, null, "PARENT", null
        );
        ResponseEntity<ParentProfileResponse> created = restTemplate.exchange(
                "/api/v1/parents", HttpMethod.POST,
                new HttpEntity<>(createReq, authHeaders()), ParentProfileResponse.class
        );
        UUID profileId = created.getBody().id();

        // Link a student
        LinkStudentRequest linkReq = new LinkStudentRequest(
                STUDENT_ID,
                "Arjun Verma",
                CENTER_ID,
                null,
                LocalDate.of(2010, 6, 15),
                "DPS Bengaluru",
                "8th",
                "CBSE",
                "ROLL-101"
        );

        ResponseEntity<StudentLinkResponse> linkResponse = restTemplate.exchange(
                "/api/v1/parents/" + profileId + "/students",
                HttpMethod.POST,
                new HttpEntity<>(linkReq, authHeaders()),
                StudentLinkResponse.class
        );

        assertThat(linkResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        StudentLinkResponse body = linkResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.parentId()).isEqualTo(profileId);
        assertThat(body.studentId()).isEqualTo(STUDENT_ID);
        assertThat(body.studentName()).isEqualTo("Arjun Verma");
        assertThat(body.centerId()).isEqualTo(CENTER_ID);
        assertThat(body.status()).isEqualTo(LinkStatus.ACTIVE);
        assertThat(body.schoolName()).isEqualTo("DPS Bengaluru");
        assertThat(body.standard()).isEqualTo("8th");
        assertThat(body.board()).isEqualTo("CBSE");
        assertThat(body.rollNumber()).isEqualTo("ROLL-101");
    }

    @Test
    @DisplayName("POST /api/v1/parents/{profileId}/students — 409 on duplicate link")
    void linkStudent_returns409_onDuplicate() {
        // Create a profile
        UUID freshUserId = UUID.randomUUID();
        UUID freshStudentId = UUID.randomUUID();
        AuthPrincipal freshPrincipal = new AuthPrincipal(freshUserId, "dup@test.com", Role.PARENT, CENTER_ID, "fp");
        given(jwtTokenValidator.validate(anyString())).willReturn(Optional.of(freshPrincipal));

        CreateParentProfileRequest createReq = new CreateParentProfileRequest(
                "Dup Parent", "+919000000044", "dup@test.com",
                null, null, null, null, "PARENT", null
        );
        ResponseEntity<ParentProfileResponse> created = restTemplate.exchange(
                "/api/v1/parents", HttpMethod.POST,
                new HttpEntity<>(createReq, authHeaders()), ParentProfileResponse.class
        );
        UUID profileId = created.getBody().id();

        // Link a student for the first time — should succeed
        LinkStudentRequest linkReq = new LinkStudentRequest(
                freshStudentId, "Dup Student", CENTER_ID,
                null, null, null, null, null, null
        );
        ResponseEntity<StudentLinkResponse> first = restTemplate.exchange(
                "/api/v1/parents/" + profileId + "/students",
                HttpMethod.POST,
                new HttpEntity<>(linkReq, authHeaders()),
                StudentLinkResponse.class
        );
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Link the same student again — must return 409
        ResponseEntity<Map> second = restTemplate.exchange(
                "/api/v1/parents/" + profileId + "/students",
                HttpMethod.POST,
                new HttpEntity<>(linkReq, authHeaders()),
                Map.class
        );
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody()).containsKey("title");
    }

    @Test
    @DisplayName("GET /api/v1/parents/{profileId}/students — 200 returns page of active links")
    void listLinkedStudents_returns200_pageOfLinks() {
        UUID freshUserId = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        AuthPrincipal freshPrincipal = new AuthPrincipal(freshUserId, "list@test.com", Role.PARENT, CENTER_ID, "fp");
        given(jwtTokenValidator.validate(anyString())).willReturn(Optional.of(freshPrincipal));

        // Create parent profile
        CreateParentProfileRequest createReq = new CreateParentProfileRequest(
                "List Parent", "+919000000055", "list@test.com",
                null, null, null, null, "PARENT", null
        );
        ResponseEntity<ParentProfileResponse> created = restTemplate.exchange(
                "/api/v1/parents", HttpMethod.POST,
                new HttpEntity<>(createReq, authHeaders()), ParentProfileResponse.class
        );
        UUID profileId = created.getBody().id();

        // Link two students
        for (UUID sid : new UUID[]{s1, s2}) {
            LinkStudentRequest lr = new LinkStudentRequest(sid, "Student " + sid, CENTER_ID,
                    null, null, null, null, null, null);
            restTemplate.exchange("/api/v1/parents/" + profileId + "/students",
                    HttpMethod.POST, new HttpEntity<>(lr, authHeaders()), StudentLinkResponse.class);
        }

        // List — Spring Page<> serializes as {"content": [...], "totalElements": N, ...}
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/parents/" + profileId + "/students",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();
        // Spring Page JSON: content array
        Object content = body.get("content");
        assertThat(content).isInstanceOf(java.util.List.class);
        assertThat(((java.util.List<?>) content)).hasSize(2);
    }

    @Test
    @DisplayName("GET /api/v1/parents/{profileId}/students — 401 when no auth header")
    void listLinkedStudents_returns401_whenUnauthenticated() {
        UUID anyProfileId = UUID.randomUUID();

        // No Authorization header
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/parents/" + anyProfileId + "/students",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class
        );

        // Spring Security returns 401 when no authentication is present
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
