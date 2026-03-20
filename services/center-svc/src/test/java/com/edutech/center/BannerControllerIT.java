// src/test/java/com/edutech/center/BannerControllerIT.java
package com.edutech.center;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BannerResponse;
import com.edutech.center.application.dto.CreateBannerRequest;
import com.edutech.center.application.dto.UpdateBannerRequest;
import com.edutech.center.domain.model.BannerAudience;
import com.edutech.center.domain.model.BannerType;
import com.edutech.center.domain.model.Role;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HTTP-level integration tests for {@link com.edutech.center.api.BannerController}.
 *
 * <p>Uses a real PostgreSQL container via TestContainers with Flyway migrations applied
 * automatically. Kafka and the JWT validator are mocked — the validator returns a
 * pre-configured {@link AuthPrincipal} for any bearer token.
 *
 * <p>Test coverage:
 * <ul>
 *   <li>POST  /api/v1/banners              — SUPER_ADMIN creates banner → 201</li>
 *   <li>POST  /api/v1/banners              — CENTER_ADMIN forbidden → 403</li>
 *   <li>GET   /api/v1/banners?audience=PARENT — active PARENT banner returned</li>
 *   <li>GET   /api/v1/banners?audience=PARENT — inactive banner NOT returned</li>
 *   <li>GET   /api/v1/banners?audience=CENTER_ADMIN — PARENT-only banner not returned</li>
 *   <li>GET   /api/v1/banners?audience=PARENT — ALL-audience banner IS returned</li>
 *   <li>GET   /api/v1/banners/all          — SUPER_ADMIN sees management view → 200</li>
 *   <li>GET   /api/v1/banners/all          — CENTER_ADMIN forbidden → 403</li>
 *   <li>PUT   /api/v1/banners/{id}         — SUPER_ADMIN updates title → 200, new title</li>
 *   <li>PUT   /api/v1/banners/{id}         — CENTER_ADMIN forbidden → 403</li>
 *   <li>PATCH /api/v1/banners/{id}/toggle  — SUPER_ADMIN toggles active=true → active=false</li>
 *   <li>DELETE /api/v1/banners/{id}        — SUPER_ADMIN deletes → 204, not in /all afterwards</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("center-svc — BannerController HTTP Integration Tests")
class BannerControllerIT {

    // -------------------------------------------------------------------------
    // Infrastructure: single shared PostgreSQL container
    // -------------------------------------------------------------------------

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("center_banner_test")
                    .withUsername("banner_user")
                    .withPassword("banner_pass");

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // -------------------------------------------------------------------------
    // Mocked infrastructure beans
    // -------------------------------------------------------------------------

    @MockBean
    @SuppressWarnings("rawtypes")
    KafkaTemplate kafkaTemplate;

    @MockBean
    JwtTokenValidator jwtTokenValidator;

    // -------------------------------------------------------------------------
    // Test collaborators
    // -------------------------------------------------------------------------

    @Autowired
    TestRestTemplate restTemplate;

    // -------------------------------------------------------------------------
    // Shared test identities
    // -------------------------------------------------------------------------

    static final UUID SUPER_ADMIN_USER_ID        = UUID.randomUUID();
    static final UUID INSTITUTION_ADMIN_USER_ID  = UUID.randomUUID();
    static final UUID CENTER_ADMIN_USER_ID       = UUID.randomUUID();
    static final UUID PARENT_USER_ID             = UUID.randomUUID();
    static final UUID CENTER_ID                  = UUID.randomUUID();

    static final String FAKE_TOKEN = "test-token";

    AuthPrincipal superAdminPrincipal;
    AuthPrincipal institutionAdminPrincipal;
    AuthPrincipal centerAdminPrincipal;
    AuthPrincipal parentPrincipal;

    @BeforeEach
    void setUp() {
        superAdminPrincipal       = new AuthPrincipal(SUPER_ADMIN_USER_ID,       "superadmin@test.com",    Role.SUPER_ADMIN,       null,      "fp-superadmin");
        institutionAdminPrincipal = new AuthPrincipal(INSTITUTION_ADMIN_USER_ID, "institution@test.com",   Role.INSTITUTION_ADMIN, null,      "fp-institution-admin");
        centerAdminPrincipal      = new AuthPrincipal(CENTER_ADMIN_USER_ID,      "admin@center.com",       Role.CENTER_ADMIN,      CENTER_ID, "fp-center-admin");
        parentPrincipal           = new AuthPrincipal(PARENT_USER_ID,            "parent@test.com",        Role.PARENT,            null,      "fp-parent");

        // Default auth: super admin
        mockAuth(superAdminPrincipal);

        // Kafka mock — suppress all publish calls
        when(kafkaTemplate.send(anyString(), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void mockAuth(AuthPrincipal principal) {
        when(jwtTokenValidator.validate(anyString())).thenReturn(Optional.of(principal));
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(FAKE_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private <T> HttpEntity<T> authEntity(T body) {
        return new HttpEntity<>(body, authHeaders());
    }

    private HttpEntity<Void> authEntity() {
        return new HttpEntity<>(authHeaders());
    }

    /**
     * Creates a banner as SUPER_ADMIN and returns its UUID.
     * The caller is responsible for setting the correct mock auth principal before calling.
     */
    private UUID createBanner(String title, BannerAudience audience) {
        CreateBannerRequest req = new CreateBannerRequest(
                title,
                "Subtitle for " + title,
                null,
                null,
                "Learn more",
                audience,
                "#4F46E5",
                1,
                null,
                null,
                BannerType.HERO
        );
        ResponseEntity<BannerResponse> resp = restTemplate.exchange(
                "/api/v1/banners",
                HttpMethod.POST,
                authEntity(req),
                BannerResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    // =========================================================================
    // Test 1: POST /api/v1/banners as SUPER_ADMIN → 201, id not null
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/banners — SUPER_ADMIN creates banner → 201, id not null")
    void createBanner_superAdmin_succeeds() {
        mockAuth(superAdminPrincipal);

        CreateBannerRequest req = new CreateBannerRequest(
                "Welcome to EduTech",
                "Platform-wide announcement",
                "https://images.edutech.com/welcome.png",
                "https://edutech.com/learn-more",
                "Explore Features",
                BannerAudience.ALL,
                "#1D4ED8",
                1,
                null,
                null,
                BannerType.HERO
        );

        ResponseEntity<BannerResponse> response = restTemplate.exchange(
                "/api/v1/banners",
                HttpMethod.POST,
                authEntity(req),
                BannerResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BannerResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.title()).isEqualTo("Welcome to EduTech");
        assertThat(body.audience()).isEqualTo(BannerAudience.ALL);
        assertThat(body.isActive()).isTrue();
        assertThat(body.createdAt()).isNotNull();
    }

    // =========================================================================
    // Test 2: POST /api/v1/banners as CENTER_ADMIN → 403
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/banners — CENTER_ADMIN creates banner → 403 Forbidden")
    void createBanner_centerAdmin_forbidden() {
        mockAuth(centerAdminPrincipal);

        CreateBannerRequest req = new CreateBannerRequest(
                "Center Admin Banner",
                null, null, null, null,
                BannerAudience.CENTER_ADMIN,
                null, 1, null, null, BannerType.HERO
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/banners",
                HttpMethod.POST,
                authEntity(req),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // Test 3: GET /api/v1/banners?audience=PARENT — active PARENT banner returned
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/banners?audience=PARENT — active PARENT banner is returned")
    void getActiveBanners_anyRole_succeeds() {
        mockAuth(superAdminPrincipal);
        String title = "Parent Active Banner " + UUID.randomUUID();
        createBanner(title, BannerAudience.PARENT);

        // Query as parent role
        mockAuth(parentPrincipal);
        ResponseEntity<List<BannerResponse>> response = restTemplate.exchange(
                "/api/v1/banners?audience=PARENT",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<BannerResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<BannerResponse> body = response.getBody();
        assertThat(body).isNotNull();
        List<String> titles = body.stream().map(BannerResponse::title).toList();
        assertThat(titles).contains(title);
    }

    // =========================================================================
    // Test 4: GET /api/v1/banners — inactive banner NOT returned
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/banners?audience=PARENT — inactive banner is NOT returned")
    void getActiveBanners_inactive_notReturned() {
        mockAuth(superAdminPrincipal);
        String inactiveTitle = "Inactive Parent Banner " + UUID.randomUUID();

        // Create and then toggle (deactivate) the banner
        UUID bannerId = createBanner(inactiveTitle, BannerAudience.PARENT);

        // Toggle to deactivate
        restTemplate.exchange(
                "/api/v1/banners/" + bannerId + "/toggle",
                HttpMethod.PATCH,
                authEntity(),
                BannerResponse.class);

        // Query active banners as parent
        mockAuth(parentPrincipal);
        ResponseEntity<List<BannerResponse>> response = restTemplate.exchange(
                "/api/v1/banners?audience=PARENT",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<BannerResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<BannerResponse> body = response.getBody();
        assertThat(body).isNotNull();
        List<String> titles = body.stream().map(BannerResponse::title).toList();
        assertThat(titles).doesNotContain(inactiveTitle);
    }

    // =========================================================================
    // Test 5: GET /api/v1/banners?audience=CENTER_ADMIN — PARENT-only banner not returned
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/banners?audience=CENTER_ADMIN — PARENT-audience banner not returned")
    void getActiveBanners_wrongAudience_notReturned() {
        mockAuth(superAdminPrincipal);
        String parentOnlyTitle = "Parent Only Banner " + UUID.randomUUID();
        createBanner(parentOnlyTitle, BannerAudience.PARENT);

        // Query CENTER_ADMIN audience
        mockAuth(centerAdminPrincipal);
        ResponseEntity<List<BannerResponse>> response = restTemplate.exchange(
                "/api/v1/banners?audience=CENTER_ADMIN",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<BannerResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<BannerResponse> body = response.getBody();
        assertThat(body).isNotNull();
        // PARENT-only banner must not appear in CENTER_ADMIN results
        List<String> titles = body.stream().map(BannerResponse::title).toList();
        assertThat(titles).doesNotContain(parentOnlyTitle);
    }

    // =========================================================================
    // Test 6: GET /api/v1/banners?audience=PARENT — ALL-audience banner IS returned
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/banners?audience=PARENT — ALL-audience banner is returned for any audience")
    void getActiveBanners_allAudience_returned() {
        mockAuth(superAdminPrincipal);
        String allTitle = "All Audience Banner " + UUID.randomUUID();
        createBanner(allTitle, BannerAudience.ALL);

        // Query as PARENT — ALL banners should appear
        mockAuth(parentPrincipal);
        ResponseEntity<List<BannerResponse>> response = restTemplate.exchange(
                "/api/v1/banners?audience=PARENT",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<BannerResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<BannerResponse> body = response.getBody();
        assertThat(body).isNotNull();
        List<String> titles = body.stream().map(BannerResponse::title).toList();
        assertThat(titles).contains(allTitle);
    }

    // =========================================================================
    // Test 7: GET /api/v1/banners/all as SUPER_ADMIN → 200
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/banners/all — SUPER_ADMIN gets management view → 200")
    void getAllBanners_superAdmin_succeeds() {
        mockAuth(superAdminPrincipal);
        // Create at least one banner to ensure list is non-empty
        createBanner("Admin View Banner " + UUID.randomUUID(), BannerAudience.ALL);

        ResponseEntity<List<BannerResponse>> response = restTemplate.exchange(
                "/api/v1/banners/all",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<BannerResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<BannerResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).isNotEmpty();
    }

    // =========================================================================
    // Test 8: GET /api/v1/banners/all as CENTER_ADMIN → 403
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/banners/all — CENTER_ADMIN gets management view → 403 Forbidden")
    void getAllBanners_nonSuperAdmin_forbidden() {
        mockAuth(centerAdminPrincipal);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/banners/all",
                HttpMethod.GET,
                authEntity(),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // Test 9: PUT /api/v1/banners/{id} as SUPER_ADMIN → 200, new title
    // =========================================================================

    @Test
    @DisplayName("PUT /api/v1/banners/{id} — SUPER_ADMIN updates title → 200, new title returned")
    void updateBanner_superAdmin_succeeds() {
        mockAuth(superAdminPrincipal);
        UUID id = createBanner("Original Banner Title", BannerAudience.PARENT);

        UpdateBannerRequest updateReq = new UpdateBannerRequest(
                "Updated Banner Title",
                "New subtitle",
                null, null, null,
                BannerAudience.PARENT,
                "#EC4899",
                2,
                null,
                null,
                null
        );

        ResponseEntity<BannerResponse> response = restTemplate.exchange(
                "/api/v1/banners/" + id,
                HttpMethod.PUT,
                authEntity(updateReq),
                BannerResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BannerResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(id);
        assertThat(body.title()).isEqualTo("Updated Banner Title");
        assertThat(body.subtitle()).isEqualTo("New subtitle");
        assertThat(body.displayOrder()).isEqualTo(2);
    }

    // =========================================================================
    // Test 10: PUT /api/v1/banners/{id} as CENTER_ADMIN → 403
    // =========================================================================

    @Test
    @DisplayName("PUT /api/v1/banners/{id} — CENTER_ADMIN updates banner → 403 Forbidden")
    void updateBanner_nonSuperAdmin_forbidden() {
        mockAuth(superAdminPrincipal);
        UUID id = createBanner("Read-only Banner", BannerAudience.CENTER_ADMIN);

        mockAuth(centerAdminPrincipal);
        UpdateBannerRequest updateReq = new UpdateBannerRequest(
                "Tampered Title",
                null, null, null, null,
                null, null, null, null, null, null
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/banners/" + id,
                HttpMethod.PUT,
                authEntity(updateReq),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // Test 11: PATCH /api/v1/banners/{id}/toggle — active=true → active=false
    // =========================================================================

    @Test
    @DisplayName("PATCH /api/v1/banners/{id}/toggle — SUPER_ADMIN toggles active=true → active=false")
    void toggleActive_superAdmin_toggles() {
        mockAuth(superAdminPrincipal);
        UUID id = createBanner("Toggle Test Banner", BannerAudience.ALL);

        // Verify it is initially active
        ResponseEntity<List<BannerResponse>> allBefore = restTemplate.exchange(
                "/api/v1/banners/all",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<BannerResponse>>() {});
        assertThat(allBefore.getStatusCode()).isEqualTo(HttpStatus.OK);
        BannerResponse before = allBefore.getBody().stream()
                .filter(b -> b.id().equals(id)).findFirst().orElse(null);
        assertThat(before).isNotNull();
        assertThat(before.isActive()).isTrue();

        // Toggle
        ResponseEntity<BannerResponse> toggleResp = restTemplate.exchange(
                "/api/v1/banners/" + id + "/toggle",
                HttpMethod.PATCH,
                authEntity(),
                BannerResponse.class);

        assertThat(toggleResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        BannerResponse toggled = toggleResp.getBody();
        assertThat(toggled).isNotNull();
        assertThat(toggled.id()).isEqualTo(id);
        assertThat(toggled.isActive()).isFalse();
    }

    // =========================================================================
    // Test 12: DELETE /api/v1/banners/{id} — 204, not in /all afterwards
    // =========================================================================

    @Test
    @DisplayName("DELETE /api/v1/banners/{id} — SUPER_ADMIN soft-deletes → 204, absent from /all")
    void deleteBanner_superAdmin_succeeds() {
        mockAuth(superAdminPrincipal);
        String deletableTitle = "Deletable Banner " + UUID.randomUUID();
        UUID id = createBanner(deletableTitle, BannerAudience.ALL);

        // Delete
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                "/api/v1/banners/" + id,
                HttpMethod.DELETE,
                authEntity(),
                Void.class);

        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify it no longer appears in the /all management view
        ResponseEntity<List<BannerResponse>> allAfter = restTemplate.exchange(
                "/api/v1/banners/all",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<BannerResponse>>() {});

        assertThat(allAfter.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> remainingTitles = allAfter.getBody().stream()
                .map(BannerResponse::title).toList();
        assertThat(remainingTitles).doesNotContain(deletableTitle);
    }

    // =========================================================================
    // Test 13: POST /api/v1/banners as INSTITUTION_ADMIN → 201
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/banners — INSTITUTION_ADMIN creates banner → 201, id not null")
    void createBanner_institutionAdmin_succeeds() {
        mockAuth(institutionAdminPrincipal);

        CreateBannerRequest req = new CreateBannerRequest(
                "Institution Admin Banner",
                "Institution-level announcement",
                null, null, "Read More",
                BannerAudience.ALL,
                "#7C3AED",
                1,
                null, null,
                BannerType.HERO
        );

        ResponseEntity<BannerResponse> response = restTemplate.exchange(
                "/api/v1/banners",
                HttpMethod.POST,
                authEntity(req),
                BannerResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BannerResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.title()).isEqualTo("Institution Admin Banner");
        assertThat(body.isActive()).isTrue();
    }

    // =========================================================================
    // Test 14: GET /api/v1/banners/all as INSTITUTION_ADMIN → 200
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/banners/all — INSTITUTION_ADMIN gets management view → 200")
    void getAllBanners_institutionAdmin_succeeds() {
        mockAuth(superAdminPrincipal);
        createBanner("Inst Admin View Banner " + UUID.randomUUID(), BannerAudience.ALL);

        mockAuth(institutionAdminPrincipal);
        ResponseEntity<List<BannerResponse>> response = restTemplate.exchange(
                "/api/v1/banners/all",
                HttpMethod.GET,
                authEntity(),
                new ParameterizedTypeReference<List<BannerResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();
    }

    // =========================================================================
    // Test 15: DELETE /api/v1/banners/{id} as INSTITUTION_ADMIN → 204
    // =========================================================================

    @Test
    @DisplayName("DELETE /api/v1/banners/{id} — INSTITUTION_ADMIN soft-deletes → 204")
    void deleteBanner_institutionAdmin_succeeds() {
        mockAuth(superAdminPrincipal);
        String title = "Inst Admin Deletable " + UUID.randomUUID();
        UUID id = createBanner(title, BannerAudience.ALL);

        mockAuth(institutionAdminPrincipal);
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                "/api/v1/banners/" + id,
                HttpMethod.DELETE,
                authEntity(),
                Void.class);

        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
