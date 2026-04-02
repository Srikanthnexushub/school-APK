package com.edutech.curricular;

import com.edutech.curricular.application.dto.ActivityCreateRequest;
import com.edutech.curricular.application.dto.ActivityResponse;
import com.edutech.curricular.application.dto.CenterCoverageReport;
import com.edutech.curricular.application.dto.ChildJourneyResponse;
import com.edutech.curricular.infrastructure.adapter.out.webclient.PerformanceMasteryAdapter;
import com.edutech.curricular.infrastructure.config.AuthPrincipal;
import com.edutech.curricular.infrastructure.config.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HTTP-level integration tests for curricular-svc controllers.
 *
 * <p>Uses a real PostgreSQL container with Flyway migrations.
 * Kafka, JWT validator, and PerformanceMasteryAdapter are mocked.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("curricular-svc — Controller HTTP Integration Tests")
class CurricularControllerIT {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("curricular_test")
            .withUsername("curricular_user")
            .withPassword("curricular_pass");

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    @SuppressWarnings("rawtypes")
    KafkaTemplate kafkaTemplate;

    @MockBean
    JwtTokenValidator jwtTokenValidator;

    @MockBean
    PerformanceMasteryAdapter performanceMasteryAdapter;

    @Autowired
    TestRestTemplate restTemplate;

    // Static immutable fixtures
    static final UUID CENTER_ID  = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID TEACHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID ADMIN_ID   = UUID.fromString("33333333-3333-3333-3333-333333333333");
    static final UUID PARENT_ID  = UUID.fromString("44444444-4444-4444-4444-444444444444");
    static final String FAKE_TOKEN = "test-token";

    static final AuthPrincipal STUDENT_PRINCIPAL =
        new AuthPrincipal(STUDENT_ID, "student@test.com", "STUDENT", null);
    static final AuthPrincipal TEACHER_PRINCIPAL =
        new AuthPrincipal(TEACHER_ID, "teacher@test.com", "TEACHER", CENTER_ID);
    static final AuthPrincipal CENTER_ADMIN_PRINCIPAL =
        new AuthPrincipal(ADMIN_ID, "admin@test.com", "CENTER_ADMIN", CENTER_ID);
    static final AuthPrincipal PARENT_PRINCIPAL =
        new AuthPrincipal(PARENT_ID, "parent@test.com", "PARENT", null);

    @BeforeEach
    void setUp() {
        when(performanceMasteryAdapter.fetchMastery(any(UUID.class), any(UUID.class), anyString()))
            .thenReturn(java.util.Map.of());
    }

    private void mockAuth(AuthPrincipal principal) {
        when(jwtTokenValidator.validate(anyString())).thenReturn(Optional.of(principal));
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(FAKE_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ─── GET /frameworks ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /frameworks -> 200 (empty list initially)")
    void listFrameworks_returns_200_empty_list() {
        mockAuth(STUDENT_PRINCIPAL);

        ResponseEntity<List> response = restTemplate.exchange(
            "/api/v1/curricular/frameworks",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /frameworks without token -> 401")
    void listFrameworks_no_auth_returns_401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/v1/curricular/frameworks",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ─── GET /activities ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /activities?centerId=xxx -> 200 empty list")
    void listActivities_returns_200_empty_list() {
        mockAuth(STUDENT_PRINCIPAL);

        ResponseEntity<List> response = restTemplate.exchange(
            "/api/v1/curricular/activities?centerId=" + CENTER_ID,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // ─── GET /my-path ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /my-path as STUDENT -> 200")
    void getMyPath_as_student_returns_200() {
        mockAuth(STUDENT_PRINCIPAL);
        UUID subjectId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();

        ResponseEntity<Map> response = restTemplate.exchange(
            "/api/v1/curricular/my-path?subjectId=" + subjectId + "&gradeId=" + gradeId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /my-path as TEACHER -> 403")
    void getMyPath_as_teacher_returns_403() {
        mockAuth(TEACHER_PRINCIPAL);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/v1/curricular/my-path?subjectId=" + UUID.randomUUID() + "&gradeId=" + UUID.randomUUID(),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ─── POST /activities ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /activities as CENTER_ADMIN -> 201")
    void createActivity_as_center_admin_returns_201() {
        mockAuth(CENTER_ADMIN_PRINCIPAL);

        ActivityCreateRequest request = new ActivityCreateRequest(
            "Chess Club", "Learn chess strategy", "OTHER", 20, "Mondays 4pm"
        );

        ResponseEntity<ActivityResponse> response = restTemplate.exchange(
            "/api/v1/curricular/activities?centerId=" + CENTER_ID,
            HttpMethod.POST,
            new HttpEntity<>(request, authHeaders()),
            ActivityResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Chess Club");
    }

    // ─── POST /activities/{id}/enroll ─────────────────────────────────────────

    @Test
    @DisplayName("POST /activities/{id}/enroll as STUDENT -> 201")
    void enrollStudent_returns_201() {
        mockAuth(CENTER_ADMIN_PRINCIPAL);

        // Create an activity first
        ActivityCreateRequest createRequest = new ActivityCreateRequest(
            "Debate Club", "Competitive debate", "DEBATE", 15, "Wednesdays 3pm"
        );
        ResponseEntity<ActivityResponse> created = restTemplate.exchange(
            "/api/v1/curricular/activities?centerId=" + CENTER_ID,
            HttpMethod.POST,
            new HttpEntity<>(createRequest, authHeaders()),
            ActivityResponse.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID activityId = created.getBody().id();

        // Enroll as student
        mockAuth(STUDENT_PRINCIPAL);
        ResponseEntity<Map> enrolled = restTemplate.exchange(
            "/api/v1/curricular/activities/" + activityId + "/enroll",
            HttpMethod.POST,
            new HttpEntity<>(authHeaders()),
            Map.class
        );

        assertThat(enrolled.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ─── GET /my-activities ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /my-activities as STUDENT -> 200 with enrollments")
    void getMyActivities_returns_200() {
        mockAuth(STUDENT_PRINCIPAL);

        ResponseEntity<List> response = restTemplate.exchange(
            "/api/v1/curricular/my-activities",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // ─── GET /center/{centerId}/report ────────────────────────────────────────

    @Test
    @DisplayName("GET /center/{centerId}/report as CENTER_ADMIN -> 200")
    void getCenterReport_returns_200() {
        mockAuth(CENTER_ADMIN_PRINCIPAL);
        UUID subjectId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();

        ResponseEntity<CenterCoverageReport> response = restTemplate.exchange(
            "/api/v1/curricular/center/" + CENTER_ID + "/report?subjectId=" + subjectId + "&gradeId=" + gradeId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            CenterCoverageReport.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ─── GET /child/{studentId}/journey ──────────────────────────────────────

    @Test
    @DisplayName("GET /child/{studentId}/journey as PARENT -> 200")
    void getChildJourney_as_parent_returns_200() {
        mockAuth(PARENT_PRINCIPAL);

        ResponseEntity<ChildJourneyResponse> response = restTemplate.exchange(
            "/api/v1/curricular/child/" + STUDENT_ID + "/journey",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            ChildJourneyResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().studentId()).isEqualTo(STUDENT_ID);
    }
}
