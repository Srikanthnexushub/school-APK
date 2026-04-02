package com.edutech.center;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.ContentItemResponse;
import com.edutech.center.application.dto.CreateCenterRequest;
import com.edutech.center.domain.model.ContentType;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HTTP-level integration tests for the external-link registration endpoint:
 * POST /api/v1/centers/{centerId}/content/link
 *
 * <p>Uses native local Postgres (env vars: POSTGRES_HOST/PORT/CENTER_SVC_DB_NAME/USERNAME/PASSWORD)
 * with Flyway migrations applied automatically. Kafka, JWT validator, DocumentStoragePort, and
 * MinioClient are mocked. A real center is created via API in @BeforeEach so that
 * ContentService.centerRepository.findById() succeeds.
 *
 * Tests (12):
 *  T01 — valid YouTube URL returns 201 with PROCESSING status
 *  T02 — title, externalUrl, type, subject, board, stream persisted correctly
 *  T03 — type=VIDEO accepted
 *  T04 — type=PDF rejected with 400 (invalid type for link endpoint)
 *  T05 — blank title returns 400
 *  T06 — STUDENT role → 403
 *  T07 — TEACHER role → 403 (cannot upload to center library)
 *  T08 — non-existent centerId → 404
 *  T09 — thumbnailUrl stored correctly
 *  T10 — tags list persisted; downloadCount=0 on fresh item
 *  T11 — minimal request (all optional fields null) succeeds
 *  T12 — registered link appears in library search GET /api/v1/library/search?centerId=...
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("center-svc — ContentLink HTTP Integration Tests")
class ContentLinkIT {

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

    static final UUID CENTER_ADMIN_USER_ID = UUID.randomUUID();
    static final UUID STUDENT_USER_ID      = UUID.randomUUID();
    static final UUID TEACHER_USER_ID      = UUID.randomUUID();
    static final UUID OWNER_ID             = UUID.randomUUID();
    static final String FAKE_TOKEN         = "link-test-token";

    UUID centerId;

    AuthPrincipal centerAdminPrincipal;
    AuthPrincipal studentPrincipal;
    AuthPrincipal teacherPrincipal;

    @BeforeEach
    void setUp() {
        // DocumentStoragePort stubs (needed even for link tests since other services may call it)
        when(documentStoragePort.buildObjectKey(any(), anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(0) + "/documents/test-uuid/" + inv.getArgument(2));
        when(documentStoragePort.generateUploadUrl(anyString(), anyString(), anyInt()))
                .thenReturn("https://minio.test/edutech-library/upload?sig=abc");
        when(documentStoragePort.generateDownloadUrl(anyString(), anyInt()))
                .thenReturn("https://minio.test/edutech-library/download?sig=xyz");

        // Create center via API as SUPER_ADMIN so centerRepository.findById() succeeds
        AuthPrincipal superAdmin = new AuthPrincipal(UUID.randomUUID(), "superadmin@link-test.com",
                Role.SUPER_ADMIN, null, "fp-link-super");
        mockAuth(superAdmin);
        centerId = createCenter("LNK" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase());

        // Per-test principals with real centerId
        centerAdminPrincipal = new AuthPrincipal(CENTER_ADMIN_USER_ID, "admin@link-test.com",
                Role.CENTER_ADMIN, centerId, "fp-link-admin");
        studentPrincipal = new AuthPrincipal(STUDENT_USER_ID, "student@link-test.com",
                Role.STUDENT, null, "fp-link-student");
        teacherPrincipal = new AuthPrincipal(TEACHER_USER_ID, "teacher@link-test.com",
                Role.TEACHER, centerId, "fp-link-teacher");

        // Default auth for tests: center admin
        mockAuth(centerAdminPrincipal);
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
    private HttpEntity<Void> authEntity()         { return new HttpEntity<>(authHeaders()); }

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

    private String linkBase() {
        return "/api/v1/centers/" + centerId + "/content/link";
    }

    /**
     * Posts a link registration request and asserts 201 CREATED.
     *
     * @return the saved ContentItemResponse
     */
    private ContentItemResponse registerLink(String title, String url, ContentType type) {
        Map<String, Object> body = Map.of(
                "title", title,
                "type", type.name(),
                "externalUrl", url
        );
        ResponseEntity<ContentItemResponse> res = restTemplate.postForEntity(
                linkBase(), authEntity(body), ContentItemResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return res.getBody();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T01 — POST /link with valid YouTube URL returns 201 with PROCESSING status")
    void t01_validYoutubeUrl_returns201WithProcessingStatus() {
        ContentItemResponse body = registerLink(
                "Physics Wave Optics — Khan Academy",
                "https://youtu.be/abc123456789",
                ContentType.LINK);

        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.status().name()).isEqualTo("PROCESSING");
        assertThat(body.centerId()).isEqualTo(centerId);
    }

    @Test
    @DisplayName("T02 — POST /link saves title, externalUrl, type, subject, board, stream correctly")
    void t02_fieldsPersistedCorrectly() {
        Map<String, Object> body = Map.of(
                "title", "NCERT Bio Ch12 — Ecology",
                "type", "LINK",
                "externalUrl", "https://youtu.be/eco_ch12_xyz",
                "subject", "Biology",
                "board", "CBSE",
                "classGrade", "12",
                "stream", "SCIENCE"
        );
        ResponseEntity<ContentItemResponse> res = restTemplate.postForEntity(
                linkBase(), authEntity(body), ContentItemResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ContentItemResponse item = res.getBody();
        assertThat(item.title()).isEqualTo("NCERT Bio Ch12 — Ecology");
        assertThat(item.type()).isEqualTo(ContentType.LINK);
        assertThat(item.subject()).isEqualTo("Biology");
        assertThat(item.board()).isEqualTo("CBSE");
        assertThat(item.classGrade()).isEqualTo("12");
        assertThat(item.stream()).isEqualTo("SCIENCE");
    }

    @Test
    @DisplayName("T03 — POST /link with type=VIDEO accepted (valid non-LINK type)")
    void t03_typeVideo_accepted() {
        ContentItemResponse body = registerLink(
                "Maths Integration — YouTube",
                "https://www.youtube.com/watch?v=integrations_xyz",
                ContentType.VIDEO);

        assertThat(body.type()).isEqualTo(ContentType.VIDEO);
        assertThat(body.id()).isNotNull();
    }

    @Test
    @DisplayName("T04 — POST /link with type=PDF rejected with 400 (invalid type for link endpoint)")
    void t04_typePdf_rejected400() {
        Map<String, Object> body = Map.of(
                "title", "Some PDF Link",
                "type", "PDF",
                "externalUrl", "https://example.com/notes.pdf"
        );
        ResponseEntity<Void> res = restTemplate.postForEntity(
                linkBase(), authEntity(body), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("T05 — POST /link with blank title returns 400")
    void t05_blankTitle_returns400() {
        Map<String, Object> body = Map.of(
                "title", "   ",
                "type", "LINK",
                "externalUrl", "https://youtu.be/abc123456789"
        );
        ResponseEntity<Void> res = restTemplate.postForEntity(
                linkBase(), authEntity(body), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("T06 — POST /link with STUDENT role → 403")
    void t06_studentRole_forbidden() {
        mockAuth(studentPrincipal);
        Map<String, Object> body = Map.of(
                "title", "Student Attempt",
                "type", "LINK",
                "externalUrl", "https://youtu.be/student_link"
        );
        ResponseEntity<Void> res = restTemplate.postForEntity(
                linkBase(), authEntity(body), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("T07 — POST /link with TEACHER role → 403 (teachers cannot upload to center library)")
    void t07_teacherRole_forbidden() {
        mockAuth(teacherPrincipal);
        Map<String, Object> body = Map.of(
                "title", "Teacher Upload Attempt",
                "type", "LINK",
                "externalUrl", "https://youtu.be/teacher_link"
        );
        ResponseEntity<Void> res = restTemplate.postForEntity(
                "/api/v1/centers/" + centerId + "/content/link",
                authEntity(body), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("T08 — POST /link for non-existent centerId → 404")
    void t08_nonExistentCenter_returns404() {
        UUID fakeCenterId = UUID.randomUUID();
        Map<String, Object> body = Map.of(
                "title", "Link for Missing Center",
                "type", "LINK",
                "externalUrl", "https://youtu.be/missing_center"
        );
        ResponseEntity<Void> res = restTemplate.postForEntity(
                "/api/v1/centers/" + fakeCenterId + "/content/link",
                authEntity(body), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("T09 — POST /link with thumbnailUrl stores it correctly")
    void t09_thumbnailUrl_storedCorrectly() {
        String thumbnailUrl = "https://img.youtube.com/vi/abc123456789/hqdefault.jpg";
        Map<String, Object> body = Map.of(
                "title", "Video with Thumbnail",
                "type", "VIDEO",
                "externalUrl", "https://youtu.be/abc123456789",
                "thumbnailUrl", thumbnailUrl
        );
        ResponseEntity<ContentItemResponse> res = restTemplate.postForEntity(
                linkBase(), authEntity(body), ContentItemResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().thumbnailUrl()).isEqualTo(thumbnailUrl);
    }

    @Test
    @DisplayName("T10 — POST /link with tags list persisted; downloadCount=0 on fresh item")
    void t10_tagsPersistedAndDownloadCountZero() {
        Map<String, Object> body = Map.of(
                "title", "Tagged Resource",
                "type", "LINK",
                "externalUrl", "https://youtu.be/tagged_resource",
                "tags", List.of("optics", "waves", "physics")
        );
        ResponseEntity<ContentItemResponse> res = restTemplate.postForEntity(
                linkBase(), authEntity(body), ContentItemResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ContentItemResponse item = res.getBody();
        assertThat(item.downloadCount()).isEqualTo(0L);
        // Tags are stored as String[] — verify not null
        assertThat(item.tags()).isNotNull();
        assertThat(item.tags()).contains("optics", "waves", "physics");
    }

    @Test
    @DisplayName("T11 — POST /link with all optional fields null succeeds (minimal request)")
    void t11_minimalRequest_succeeds() {
        Map<String, Object> body = Map.of(
                "title", "Minimal Link Resource",
                "type", "LINK",
                "externalUrl", "https://example.com/minimal"
        );
        ResponseEntity<ContentItemResponse> res = restTemplate.postForEntity(
                linkBase(), authEntity(body), ContentItemResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ContentItemResponse item = res.getBody();
        assertThat(item.id()).isNotNull();
        assertThat(item.title()).isEqualTo("Minimal Link Resource");
        assertThat(item.subject()).isNull();
        assertThat(item.board()).isNull();
        assertThat(item.description()).isNull();
    }

    @Test
    @DisplayName("T12 — POST /link content appears in library search GET /api/v1/library/search?centerId=...")
    void t12_registeredLink_appearsInLibrarySearch() {
        String uniqueTitle = "Unique-Link-" + UUID.randomUUID().toString().substring(0, 8);
        registerLink(uniqueTitle, "https://youtu.be/search_test_xyz", ContentType.LINK);

        // Search by centerId and title fragment
        ResponseEntity<Map> searchRes = restTemplate.exchange(
                "/api/v1/library/search?centerId=" + centerId + "&q=" + uniqueTitle,
                HttpMethod.GET, authEntity(), Map.class);

        assertThat(searchRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(searchRes.getBody()).containsKey("content");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) searchRes.getBody().get("content");
        boolean found = content.stream()
                .anyMatch(c -> uniqueTitle.equals(c.get("title")));
        assertThat(found).as("Registered link '%s' should appear in library search results", uniqueTitle).isTrue();
    }
}
