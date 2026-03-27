package com.edutech.center;

import com.edutech.center.application.command.SendAnnouncementCommand;
import com.edutech.center.application.dto.AnnouncementResponse;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.CreateCenterRequest;
import com.edutech.center.domain.model.AnnouncementTargetType;
import com.edutech.center.domain.model.Role;
import com.edutech.center.domain.port.out.AiTaggingPort;
import com.edutech.center.domain.port.out.DocumentStoragePort;
import com.edutech.center.infrastructure.security.JwtTokenValidator;
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
import com.edutech.center.domain.port.out.CenterEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HTTP-level integration tests for {@link com.edutech.center.api.AnnouncementController}.
 *
 * <p>Uses native local Postgres (env vars: POSTGRES_HOST/PORT/CENTER_SVC_DB_NAME/USERNAME/PASSWORD)
 * with Flyway migrations applied automatically. Kafka and the JWT validator are mocked.
 *
 * <p>Test coverage (8 tests):
 * <ul>
 *   <li>POST  /centers/{id}/announcements — CENTER_ADMIN creates CENTER-scoped → 201</li>
 *   <li>POST  /centers/{id}/announcements — STUDENT role → 403</li>
 *   <li>POST  /centers/{id}/announcements — BATCH-targeted announcement → 201, targetId set</li>
 *   <li>POST  /centers/{id}/announcements — scheduled announcement → 201, sentAt null</li>
 *   <li>GET   /centers/{id}/announcements — lists active announcements → 200</li>
 *   <li>GET   /centers/{id}/announcements — expired announcement not listed</li>
 *   <li>DELETE /centers/{id}/announcements/{annId} — CENTER_ADMIN expires → 204</li>
 *   <li>POST  /centers/{id}/announcements — INSTITUTION_ADMIN creates → 201</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("center-svc — AnnouncementController HTTP Integration Tests")
class AnnouncementControllerIT {

    @MockBean
    CenterEventPublisher centerEventPublisher;

    @MockBean
    JwtTokenValidator jwtTokenValidator;

    @MockBean
    DocumentStoragePort documentStoragePort;

    @MockBean
    io.minio.MinioClient minioClient;

    @MockBean
    AiTaggingPort aiTaggingPort;

    @Autowired
    TestRestTemplate restTemplate;

    static final UUID SUPER_ADMIN_ID       = UUID.randomUUID();
    static final UUID INSTITUTION_ADMIN_ID = UUID.randomUUID();
    static final UUID CENTER_ADMIN_ID      = UUID.randomUUID();
    static final UUID STUDENT_ID           = UUID.randomUUID();
    static final UUID OWNER_ID             = UUID.randomUUID();
    static final String FAKE_TOKEN         = "test-token";

    AuthPrincipal superAdminPrincipal;
    AuthPrincipal institutionAdminPrincipal;
    AuthPrincipal centerAdminPrincipal;
    AuthPrincipal studentPrincipal;

    // centerId shared across tests (created once per test via helper)
    UUID centerId;

    @BeforeEach
    void setUp() {
        superAdminPrincipal       = new AuthPrincipal(SUPER_ADMIN_ID,       "sa@test.com",   Role.SUPER_ADMIN,       null,      "fp-sa");
        institutionAdminPrincipal = new AuthPrincipal(INSTITUTION_ADMIN_ID, "ia@test.com",   Role.INSTITUTION_ADMIN, null,      "fp-ia");

        // create a fresh center per test to avoid cross-test pollution
        mockAuth(superAdminPrincipal);
        centerId = createCenter("ANN" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        centerAdminPrincipal = new AuthPrincipal(CENTER_ADMIN_ID, "ca@test.com", Role.CENTER_ADMIN, centerId, "fp-ca");
        studentPrincipal     = new AuthPrincipal(STUDENT_ID,      "st@test.com", Role.STUDENT,      centerId, "fp-st");

    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void mockAuth(AuthPrincipal principal) {
        when(jwtTokenValidator.validate(anyString())).thenReturn(Optional.of(principal));
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(FAKE_TOKEN);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private <T> HttpEntity<T> authEntity(T body) { return new HttpEntity<>(body, authHeaders()); }
    private HttpEntity<Void>  authEntity()        { return new HttpEntity<>(authHeaders()); }

    private UUID createCenter(String code) {
        CreateCenterRequest req = new CreateCenterRequest(
                "Test Center " + code, code,
                "123 Test St", "Bangalore", "Karnataka", "560001",
                "9876543210", code.toLowerCase() + "@test.com",
                null, null, OWNER_ID, null);
        ResponseEntity<CenterResponse> r = restTemplate.exchange(
                "/api/v1/centers", HttpMethod.POST, authEntity(req), CenterResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().id();
    }

    private UUID sendAnnouncement(String title, AnnouncementTargetType targetType) {
        SendAnnouncementCommand cmd = new SendAnnouncementCommand(
                centerId, title, "Body for " + title,
                targetType, null, null, null, null);
        ResponseEntity<AnnouncementResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/announcements",
                HttpMethod.POST, authEntity(cmd), AnnouncementResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().id();
    }

    // =========================================================================
    // Test 1: CENTER_ADMIN creates CENTER-scoped announcement → 201
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/announcements — CENTER_ADMIN creates CENTER-scoped → 201, id not null")
    void createAnnouncement_centerAdmin_succeeds() {
        mockAuth(centerAdminPrincipal);

        SendAnnouncementCommand cmd = new SendAnnouncementCommand(
                centerId, "End-of-term Results", "All results are now published.",
                AnnouncementTargetType.CENTER, null, null, null, null);

        ResponseEntity<AnnouncementResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/announcements",
                HttpMethod.POST, authEntity(cmd), AnnouncementResponse.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AnnouncementResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.title()).isEqualTo("End-of-term Results");
        assertThat(body.centerId()).isEqualTo(centerId);
        assertThat(body.targetType()).isEqualTo(AnnouncementTargetType.CENTER);
        assertThat(body.createdAt()).isNotNull();
    }

    // =========================================================================
    // Test 2: STUDENT role → 403
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/announcements — STUDENT role → 403 Forbidden")
    void createAnnouncement_student_forbidden() {
        mockAuth(studentPrincipal);

        SendAnnouncementCommand cmd = new SendAnnouncementCommand(
                centerId, "Student Hack", "Should not be allowed.",
                AnnouncementTargetType.CENTER, null, null, null, null);

        ResponseEntity<String> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/announcements",
                HttpMethod.POST, authEntity(cmd), String.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // Test 3: BATCH-targeted announcement → 201, targetId preserved
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/announcements — BATCH target → 201, targetId stored")
    void createAnnouncement_batchTarget_targetIdStored() {
        mockAuth(centerAdminPrincipal);
        UUID fakeBatchId = UUID.randomUUID();

        SendAnnouncementCommand cmd = new SendAnnouncementCommand(
                centerId, "Batch Exam Reminder", "Your exam is next week.",
                AnnouncementTargetType.BATCH, fakeBatchId, null, null, null);

        ResponseEntity<AnnouncementResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/announcements",
                HttpMethod.POST, authEntity(cmd), AnnouncementResponse.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AnnouncementResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.targetType()).isEqualTo(AnnouncementTargetType.BATCH);
        assertThat(body.targetId()).isEqualTo(fakeBatchId);
    }

    // =========================================================================
    // Test 4: Scheduled announcement → 201, sentAt is null
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/announcements — scheduled → 201, sentAt null until processing")
    void createAnnouncement_scheduled_sentAtNull() {
        mockAuth(centerAdminPrincipal);

        java.time.Instant futureTime = java.time.Instant.now().plusSeconds(3600);
        SendAnnouncementCommand cmd = new SendAnnouncementCommand(
                centerId, "Upcoming Holiday Notice", "School closed next Monday.",
                AnnouncementTargetType.CENTER, null, null, futureTime, null);

        ResponseEntity<AnnouncementResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/announcements",
                HttpMethod.POST, authEntity(cmd), AnnouncementResponse.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AnnouncementResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.scheduledAt()).isNotNull();
        // Scheduled announcements should not be immediately marked as sent
        assertThat(body.sentAt()).isNull();
    }

    // =========================================================================
    // Test 5: GET lists active announcements → 200
    // =========================================================================

    @Test
    @DisplayName("GET /centers/{id}/announcements — returns list with created announcement")
    void listAnnouncements_returnsCreatedAnnouncement() {
        mockAuth(centerAdminPrincipal);
        String title = "Fee Reminder " + UUID.randomUUID();
        sendAnnouncement(title, AnnouncementTargetType.CENTER);

        ResponseEntity<List<AnnouncementResponse>> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/announcements",
                HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<AnnouncementResponse>>() {});

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> titles = r.getBody().stream().map(AnnouncementResponse::title).toList();
        assertThat(titles).contains(title);
    }

    // =========================================================================
    // Test 6: Expired announcement not listed
    // =========================================================================

    @Test
    @DisplayName("DELETE then GET — expired announcement absent from active list")
    void expiredAnnouncement_notInList() {
        mockAuth(centerAdminPrincipal);
        String title = "Soon Expired " + UUID.randomUUID();
        UUID annId = sendAnnouncement(title, AnnouncementTargetType.CENTER);

        // Expire it
        ResponseEntity<Void> del = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/announcements/" + annId,
                HttpMethod.DELETE, authEntity(), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify not in active list
        ResponseEntity<List<AnnouncementResponse>> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/announcements",
                HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<AnnouncementResponse>>() {});

        List<String> titles = r.getBody().stream().map(AnnouncementResponse::title).toList();
        assertThat(titles).doesNotContain(title);
    }

    // =========================================================================
    // Test 7: DELETE announcement → 204
    // =========================================================================

    @Test
    @DisplayName("DELETE /centers/{id}/announcements/{annId} — CENTER_ADMIN expires → 204")
    void deleteAnnouncement_centerAdmin_succeeds() {
        mockAuth(centerAdminPrincipal);
        UUID annId = sendAnnouncement("Deletable Ann " + UUID.randomUUID(), AnnouncementTargetType.ALL);

        ResponseEntity<Void> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/announcements/" + annId,
                HttpMethod.DELETE, authEntity(), Void.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // =========================================================================
    // Test 8: INSTITUTION_ADMIN creates announcement → 201
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/announcements — INSTITUTION_ADMIN creates → 201")
    void createAnnouncement_institutionAdmin_succeeds() {
        mockAuth(institutionAdminPrincipal);

        SendAnnouncementCommand cmd = new SendAnnouncementCommand(
                centerId, "Platform Maintenance", "System down 2am–4am Saturday.",
                AnnouncementTargetType.ALL, null, null, null, null);

        ResponseEntity<AnnouncementResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/announcements",
                HttpMethod.POST, authEntity(cmd), AnnouncementResponse.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getBody()).isNotNull();
        assertThat(r.getBody().id()).isNotNull();
        assertThat(r.getBody().targetType()).isEqualTo(AnnouncementTargetType.ALL);
    }
}
