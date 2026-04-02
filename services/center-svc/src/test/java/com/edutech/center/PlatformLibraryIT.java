package com.edutech.center;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.ContentItemResponse;
import com.edutech.center.application.dto.CreateCenterRequest;
import com.edutech.center.domain.model.Role;
import com.edutech.center.domain.port.out.AiTaggingPort;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.domain.port.out.DocumentStoragePort;
import com.edutech.center.infrastructure.security.JwtTokenValidator;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HTTP-level integration tests for the platform-wide link registration endpoint:
 * POST /api/v1/platform/content/link
 *
 * <p>Only SUPER_ADMIN and INSTITUTION_ADMIN may call this endpoint. All other roles receive 403.
 * Platform links have centerId=null and platformResource=true.
 *
 * Tests (10):
 *  T01 — SUPER_ADMIN can POST → 201
 *  T02 — INSTITUTION_ADMIN can POST → 201
 *  T03 — CENTER_ADMIN cannot POST → 403
 *  T04 — STUDENT cannot POST → 403
 *  T05 — TEACHER cannot POST → 403
 *  T06 — Platform link response has centerId = null
 *  T07 — Platform link response has platformResource = true
 *  T08 — title, board, classGrade, stream persisted correctly
 *  T09 — blank title returns 400
 *  T10 — type=PDF returns 400
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("center-svc — PlatformLibrary HTTP Integration Tests")
class PlatformLibraryIT {

    // ── Mocked beans ──────────────────────────────────────────────────────────

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

    // ── Test fixtures ─────────────────────────────────────────────────────────

    @Autowired
    TestRestTemplate restTemplate;

    static final UUID SUPER_ADMIN_USER_ID        = UUID.randomUUID();
    static final UUID INSTITUTION_ADMIN_USER_ID  = UUID.randomUUID();
    static final UUID CENTER_ADMIN_USER_ID        = UUID.randomUUID();
    static final UUID STUDENT_USER_ID             = UUID.randomUUID();
    static final UUID TEACHER_USER_ID             = UUID.randomUUID();
    static final UUID OWNER_ID                    = UUID.randomUUID();
    static final String FAKE_TOKEN                = "platform-lib-test-token";

    /** centerId used for CENTER_ADMIN principal — must exist in DB for auth to pass filter */
    UUID centerId;

    AuthPrincipal superAdminPrincipal;
    AuthPrincipal institutionAdminPrincipal;
    AuthPrincipal centerAdminPrincipal;
    AuthPrincipal studentPrincipal;
    AuthPrincipal teacherPrincipal;

    @BeforeEach
    void setUp() {
        // DocumentStoragePort stubs
        when(documentStoragePort.buildObjectKey(any(), anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(0) + "/documents/test-uuid/" + inv.getArgument(2));
        when(documentStoragePort.generateUploadUrl(anyString(), anyString(), anyInt()))
                .thenReturn("https://minio.test/edutech-library/upload?sig=abc");
        when(documentStoragePort.generateDownloadUrl(anyString(), anyInt()))
                .thenReturn("https://minio.test/edutech-library/download?sig=xyz");

        // Create center via API as SUPER_ADMIN — provides a real centerId for CENTER_ADMIN principal
        AuthPrincipal setupAdmin = new AuthPrincipal(UUID.randomUUID(), "setup@platform-lib-test.com",
                Role.SUPER_ADMIN, null, "fp-platform-setup");
        mockAuth(setupAdmin);
        centerId = createCenter("PLT" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase());

        // Build all role principals
        superAdminPrincipal = new AuthPrincipal(SUPER_ADMIN_USER_ID, "superadmin@platform-lib-test.com",
                Role.SUPER_ADMIN, null, "fp-platform-super");
        institutionAdminPrincipal = new AuthPrincipal(INSTITUTION_ADMIN_USER_ID, "ia@platform-lib-test.com",
                Role.INSTITUTION_ADMIN, null, "fp-platform-ia");
        centerAdminPrincipal = new AuthPrincipal(CENTER_ADMIN_USER_ID, "ca@platform-lib-test.com",
                Role.CENTER_ADMIN, centerId, "fp-platform-ca");
        studentPrincipal = new AuthPrincipal(STUDENT_USER_ID, "student@platform-lib-test.com",
                Role.STUDENT, null, "fp-platform-student");
        teacherPrincipal = new AuthPrincipal(TEACHER_USER_ID, "teacher@platform-lib-test.com",
                Role.TEACHER, centerId, "fp-platform-teacher");

        // Default auth for tests: SUPER_ADMIN
        mockAuth(superAdminPrincipal);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private UUID createCenter(String code) {
        CreateCenterRequest req = new CreateCenterRequest(
                "Test Center " + code, code,
                "1 Main St", "Bangalore", "Karnataka", "560001",
                "9876543210", code.toLowerCase() + "@test.com",
                null, null, OWNER_ID, null);
        ResponseEntity<CenterResponse> r = restTemplate.exchange(
                "/api/v1/centers", HttpMethod.POST, authEntity(req), CenterResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().id();
    }

    private static final String PLATFORM_LINK_URL = "/api/v1/platform/content/link";

    /** Builds a minimal valid platform link request body. */
    private Map<String, Object> minimalPlatformLinkBody(String title) {
        return Map.of(
                "title", title,
                "type", "LINK",
                "externalUrl", "https://youtu.be/platform_resource_xyz"
        );
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T01 — SUPER_ADMIN can POST /api/v1/platform/content/link → 201")
    void t01_superAdmin_canCreatePlatformLink() {
        mockAuth(superAdminPrincipal);
        ResponseEntity<ContentItemResponse> res = restTemplate.postForEntity(
                PLATFORM_LINK_URL, authEntity(minimalPlatformLinkBody("SA Platform Link")),
                ContentItemResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().id()).isNotNull();
    }

    @Test
    @DisplayName("T02 — INSTITUTION_ADMIN can POST /api/v1/platform/content/link → 201")
    void t02_institutionAdmin_canCreatePlatformLink() {
        mockAuth(institutionAdminPrincipal);
        ResponseEntity<ContentItemResponse> res = restTemplate.postForEntity(
                PLATFORM_LINK_URL, authEntity(minimalPlatformLinkBody("IA Platform Link")),
                ContentItemResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().id()).isNotNull();
    }

    @Test
    @DisplayName("T03 — CENTER_ADMIN cannot POST /api/v1/platform/content/link → 403")
    void t03_centerAdmin_forbidden() {
        mockAuth(centerAdminPrincipal);
        ResponseEntity<Void> res = restTemplate.postForEntity(
                PLATFORM_LINK_URL, authEntity(minimalPlatformLinkBody("CA Attempt")), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("T04 — STUDENT cannot POST /api/v1/platform/content/link → 403")
    void t04_student_forbidden() {
        mockAuth(studentPrincipal);
        ResponseEntity<Void> res = restTemplate.postForEntity(
                PLATFORM_LINK_URL, authEntity(minimalPlatformLinkBody("Student Attempt")), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("T05 — TEACHER cannot POST /api/v1/platform/content/link → 403")
    void t05_teacher_forbidden() {
        mockAuth(teacherPrincipal);
        ResponseEntity<Void> res = restTemplate.postForEntity(
                PLATFORM_LINK_URL, authEntity(minimalPlatformLinkBody("Teacher Attempt")), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("T06 — Platform link response has centerId = null (platform-wide)")
    void t06_platformLink_centerIdIsNull() {
        mockAuth(superAdminPrincipal);
        ResponseEntity<ContentItemResponse> res = restTemplate.postForEntity(
                PLATFORM_LINK_URL, authEntity(minimalPlatformLinkBody("No-Center Link")),
                ContentItemResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().centerId())
                .as("Platform link must not be scoped to any center")
                .isNull();
    }

    @Test
    @DisplayName("T07 — Platform link response has platformResource = true")
    void t07_platformLink_isPlatformResourceTrue() {
        mockAuth(superAdminPrincipal);
        ResponseEntity<ContentItemResponse> res = restTemplate.postForEntity(
                PLATFORM_LINK_URL, authEntity(minimalPlatformLinkBody("Platform Resource")),
                ContentItemResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().platformResource())
                .as("platformResource flag must be true for platform-wide links")
                .isTrue();
    }

    @Test
    @DisplayName("T08 — title, board, classGrade, stream persisted correctly for platform link")
    void t08_metadata_persistedCorrectly() {
        mockAuth(superAdminPrincipal);
        Map<String, Object> body = Map.of(
                "title", "CBSE Class 12 Physics — Magnetism",
                "type", "LINK",
                "externalUrl", "https://youtu.be/magnetism_cbse_12",
                "board", "CBSE",
                "classGrade", "12",
                "stream", "SCIENCE",
                "subject", "Physics"
        );
        ResponseEntity<ContentItemResponse> res = restTemplate.postForEntity(
                PLATFORM_LINK_URL, authEntity(body), ContentItemResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ContentItemResponse item = res.getBody();
        assertThat(item.title()).isEqualTo("CBSE Class 12 Physics — Magnetism");
        assertThat(item.board()).isEqualTo("CBSE");
        assertThat(item.classGrade()).isEqualTo("12");
        assertThat(item.stream()).isEqualTo("SCIENCE");
    }

    @Test
    @DisplayName("T09 — POST /api/v1/platform/content/link with blank title returns 400")
    void t09_blankTitle_returns400() {
        mockAuth(superAdminPrincipal);
        Map<String, Object> body = Map.of(
                "title", "   ",
                "type", "LINK",
                "externalUrl", "https://youtu.be/blank_title"
        );
        ResponseEntity<Void> res = restTemplate.postForEntity(
                PLATFORM_LINK_URL, authEntity(body), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("T10 — POST /api/v1/platform/content/link with type=PDF returns 400")
    void t10_typePdf_returns400() {
        mockAuth(superAdminPrincipal);
        Map<String, Object> body = Map.of(
                "title", "PDF Link Attempt",
                "type", "PDF",
                "externalUrl", "https://example.com/notes.pdf"
        );
        ResponseEntity<Void> res = restTemplate.postForEntity(
                PLATFORM_LINK_URL, authEntity(body), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
