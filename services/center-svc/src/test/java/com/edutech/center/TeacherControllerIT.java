// src/test/java/com/edutech/center/TeacherControllerIT.java
package com.edutech.center;

import com.edutech.center.application.dto.AcceptInvitationRequest;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BulkImportConfirmResponse;
import com.edutech.center.application.dto.BulkImportPreviewResponse;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.CreateCenterRequest;
import com.edutech.center.application.dto.CreateStaffRequest;
import com.edutech.center.application.dto.InvitationLookupResponse;
import com.edutech.center.application.dto.TeacherResponse;
import com.edutech.center.application.dto.TeacherSelfRegisterRequest;
import com.edutech.center.application.dto.UpdateStaffRequest;
import com.edutech.center.domain.model.Role;
import com.edutech.center.domain.model.StaffRoleType;
import com.edutech.center.domain.model.TeacherStatus;
import com.edutech.center.domain.port.out.TeacherRepository;
import com.edutech.center.infrastructure.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HTTP-level integration tests for 3.4 Teacher Onboarding endpoints.
 *
 * <p>Covers:
 * <ul>
 *   <li>GET  /centers/{id}/teachers/bulk-template           — CSV template download</li>
 *   <li>POST /centers/{id}/teachers/bulk-preview            — CSV validation preview</li>
 *   <li>POST /centers/{id}/teachers/bulk-confirm            — CSV import + stubs created</li>
 *   <li>POST /centers/{id}/teachers/self-register           — teacher self-registration</li>
 *   <li>GET  /centers/{id}/teachers/pending                 — list pending approvals</li>
 *   <li>POST /centers/{id}/teachers/{tid}/approve           — approve pending teacher</li>
 *   <li>POST /centers/{id}/teachers/{tid}/reject            — reject pending teacher</li>
 *   <li>POST /centers/{id}/teachers/accept-invitation       — link stub to userId</li>
 *   <li>GET  /api/v1/teachers/invitation/{token}            — public token lookup</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("center-svc — Teacher Onboarding HTTP Integration Tests")
class TeacherControllerIT {

    // ─── Infrastructure ───────────────────────────────────────────────────────

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("teacher_ctrl_test")
                    .withUsername("center_user")
                    .withPassword("center_pass");

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // ─── Mocked infrastructure beans ──────────────────────────────────────────

    @MockBean
    @SuppressWarnings("rawtypes")
    KafkaTemplate kafkaTemplate;

    @MockBean
    JwtTokenValidator jwtTokenValidator;

    // ─── Collaborators ────────────────────────────────────────────────────────

    @Autowired TestRestTemplate restTemplate;

    /** Injected to retrieve invitation tokens from stubs after bulk-confirm. */
    @Autowired TeacherRepository teacherRepository;

    // ─── Shared identities ────────────────────────────────────────────────────

    static final UUID OWNER_ID    = UUID.randomUUID();
    static final UUID TEACHER_UID = UUID.randomUUID();
    static final String FAKE_TOKEN = "test-token";

    AuthPrincipal superAdminPrincipal;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        superAdminPrincipal = new AuthPrincipal(OWNER_ID, "super@nexused.dev",
                Role.SUPER_ADMIN, null, "fp-super");
        mockAuth(superAdminPrincipal);

        when(kafkaTemplate.send(anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void mockAuth(AuthPrincipal principal) {
        when(jwtTokenValidator.validate(anyString())).thenReturn(Optional.of(principal));
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(FAKE_TOKEN);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private <T> HttpEntity<T> authEntity(T body) {
        return new HttpEntity<>(body, authHeaders());
    }

    private HttpEntity<Void> authEntity() {
        return new HttpEntity<>(authHeaders());
    }

    /** Multipart entity for CSV upload (uses ByteArrayResource to carry a filename). */
    private HttpEntity<MultiValueMap<String, Object>> csvUploadEntity(String csvContent) {
        ByteArrayResource resource = new ByteArrayResource(
                csvContent.getBytes(StandardCharsets.UTF_8)) {
            @Override public String getFilename() { return "teachers.csv"; }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(FAKE_TOKEN);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(body, headers);
    }

    /** Create a fresh center and return its id. */
    private UUID createCenter(String code) {
        CreateCenterRequest req = new CreateCenterRequest(
                "Teacher IT Academy " + code, code,
                "123 Main St", "Bengaluru", "Karnataka", "560001",
                "9876500000", "info@" + code.toLowerCase() + ".com",
                null, null, OWNER_ID);
        CenterResponse center = restTemplate.exchange(
                "/api/v1/centers", HttpMethod.POST, authEntity(req), CenterResponse.class).getBody();
        return center.id();
    }

    private static final String VALID_CSV =
            "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
            "Ravi,Kumar,ravi.%s@school.com,+919876543210,Mathematics,T-001\n";

    // =========================================================================
    // 1. GET bulk-template — CSV download
    // =========================================================================

    @Test
    @DisplayName("GET /centers/{id}/teachers/bulk-template — returns 200 with text/csv attachment")
    void getBulkTemplate_returns200WithCsvContent() {
        UUID centerId = createCenter("TMPL01");
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@tmpl.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/bulk-template",
                HttpMethod.GET, authEntity(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("text/csv");
        String body = response.getBody();
        assertThat(body).contains("First Name");
        assertThat(body).contains("Email");
        assertThat(body).contains("Subjects");
    }

    // =========================================================================
    // 2. POST bulk-preview — valid CSV
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/teachers/bulk-preview — valid CSV returns 200 with no errors")
    void bulkPreview_validCsv_returns200NoErrors() {
        UUID centerId = createCenter("PRV01");
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@prv.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,ravi.prv01@school.com,+919876543210,Mathematics,T-001\n";
        ResponseEntity<BulkImportPreviewResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/bulk-preview",
                HttpMethod.POST, csvUploadEntity(csv), BulkImportPreviewResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BulkImportPreviewResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.totalRows()).isEqualTo(1);
        assertThat(body.validRows()).isEqualTo(1);
        assertThat(body.errorRows()).isEqualTo(0);
        assertThat(body.errors()).isEmpty();
    }

    @Test
    @DisplayName("POST /centers/{id}/teachers/bulk-preview — invalid email row returns 200 with errors list")
    void bulkPreview_invalidEmail_returns200WithErrors() {
        UUID centerId = createCenter("PRV02");
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@prv2.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,not-an-email,+91987,Mathematics,T-001\n";
        ResponseEntity<BulkImportPreviewResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/bulk-preview",
                HttpMethod.POST, csvUploadEntity(csv), BulkImportPreviewResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().errorRows()).isEqualTo(1);
        assertThat(response.getBody().errors()).isNotEmpty();
        assertThat(response.getBody().errors().get(0).field()).isEqualTo("email");
    }

    // =========================================================================
    // 3. POST bulk-confirm — creates invitation stubs
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/teachers/bulk-confirm — valid CSV returns 201 with imported count")
    void bulkConfirm_validCsv_returns201() {
        UUID centerId = createCenter("CNF01");
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@cnf.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,ravi.cnf01@school.com,+919876543210,Mathematics,T-001\n";
        ResponseEntity<BulkImportConfirmResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/bulk-confirm",
                HttpMethod.POST, csvUploadEntity(csv), BulkImportConfirmResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BulkImportConfirmResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.imported()).isEqualTo(1);
        assertThat(body.skipped()).isEqualTo(0);
    }

    @Test
    @DisplayName("POST /centers/{id}/teachers/bulk-confirm — CSV with errors and skipErrors=false returns 400")
    void bulkConfirm_withErrors_skipFalse_returns400() {
        UUID centerId = createCenter("CNF02");
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@cnf2.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,bad-email,+91987,Mathematics,T-001\n";

        // Append skipErrors=false query param (default, but explicit)
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/bulk-confirm?skipErrors=false",
                HttpMethod.POST, csvUploadEntity(csv), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // 4. GET invitation/{token} — public lookup
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/teachers/invitation/{token} — valid token returns 200 with teacher info")
    void invitationLookup_validToken_returns200() {
        // Setup: create center + confirm CSV import to create a stub
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("INV01");
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@inv.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        String uniqueEmail = "ravi.inv01@school.com";
        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar," + uniqueEmail + ",+919876543210,Mathematics,T-001\n";
        restTemplate.exchange("/api/v1/centers/" + centerId + "/teachers/bulk-confirm",
                HttpMethod.POST, csvUploadEntity(csv), BulkImportConfirmResponse.class);

        // Fetch the created teacher stub via the domain port to get its invitation token
        String invitationToken = teacherRepository.findByCenterId(centerId).stream()
                .filter(t -> t.getEmail().equals(uniqueEmail))
                .map(com.edutech.center.domain.model.Teacher::getInvitationToken)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Teacher stub not found"));

        // Call the public endpoint — no Authorization header required
        ResponseEntity<InvitationLookupResponse> response = restTemplate.exchange(
                "/api/v1/teachers/invitation/" + invitationToken,
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), InvitationLookupResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        InvitationLookupResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.email()).isEqualTo(uniqueEmail);
        assertThat(body.firstName()).isEqualTo("Ravi");
        assertThat(body.lastName()).isEqualTo("Kumar");
        assertThat(body.centerId()).isEqualTo(centerId);
        assertThat(body.centerName()).isEqualTo("Teacher IT Academy INV01");
    }

    @Test
    @DisplayName("GET /api/v1/teachers/invitation/{token} — unknown token returns 404")
    void invitationLookup_unknownToken_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/teachers/invitation/no-such-token-uuid",
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // =========================================================================
    // 5. POST self-register — teacher self-registers to a center
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/teachers/self-register — returns 201 with PENDING_APPROVAL status")
    void selfRegister_returns201WithPendingStatus() {
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("SR01");

        // Switch to teacher principal for self-register
        AuthPrincipal teacher = new AuthPrincipal(TEACHER_UID, "teacher.sr01@school.com",
                Role.TEACHER, null, "fp-teacher");
        mockAuth(teacher);

        TeacherSelfRegisterRequest request = new TeacherSelfRegisterRequest(
                "Meera", "Nair", "meera.sr01@school.com", "+919876543299", "Chemistry", null);

        ResponseEntity<TeacherResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/self-register",
                HttpMethod.POST, authEntity(request), TeacherResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TeacherResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.firstName()).isEqualTo("Meera");
        assertThat(body.status()).isEqualTo(TeacherStatus.PENDING_APPROVAL);
        assertThat(body.userId()).isEqualTo(TEACHER_UID);
    }

    @Test
    @DisplayName("POST /centers/{id}/teachers/self-register — duplicate self-registration returns 409")
    void selfRegister_duplicate_returns409() {
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("SR02");

        UUID teacherUid = UUID.randomUUID();
        AuthPrincipal teacher = new AuthPrincipal(teacherUid, "teacher.sr02@school.com",
                Role.TEACHER, null, "fp-teacher");
        mockAuth(teacher);

        TeacherSelfRegisterRequest request = new TeacherSelfRegisterRequest(
                "Arjun", "Singh", "arjun.sr02@school.com", null, "History", null);

        // First registration
        restTemplate.exchange("/api/v1/centers/" + centerId + "/teachers/self-register",
                HttpMethod.POST, authEntity(request), TeacherResponse.class);

        // Duplicate
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/self-register",
                HttpMethod.POST, authEntity(request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // =========================================================================
    // 6. GET pending — list pending approvals
    // =========================================================================

    @Test
    @DisplayName("GET /centers/{id}/teachers/pending — CENTER_ADMIN returns 200 with pending list")
    void listPending_returns200WithPendingTeachers() {
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("PEND01");

        // Self-register a teacher
        UUID teacherUid = UUID.randomUUID();
        AuthPrincipal teacher = new AuthPrincipal(teacherUid, "pending01@school.com",
                Role.TEACHER, null, "fp-t");
        mockAuth(teacher);
        restTemplate.exchange("/api/v1/centers/" + centerId + "/teachers/self-register",
                HttpMethod.POST,
                authEntity(new TeacherSelfRegisterRequest("P", "End", "pending01@school.com", null, "Economics", null)),
                TeacherResponse.class);

        // Switch to admin to list pending
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@pend.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        ResponseEntity<TeacherResponse[]> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/pending",
                HttpMethod.GET, authEntity(), TeacherResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].status()).isEqualTo(TeacherStatus.PENDING_APPROVAL);
    }

    // =========================================================================
    // 7. POST approve — approve a pending teacher
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/teachers/{tid}/approve — returns 200 with ACTIVE status")
    void approve_returns200WithActiveStatus() {
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("APR01");

        // Self-register
        UUID teacherUid = UUID.randomUUID();
        AuthPrincipal teacher = new AuthPrincipal(teacherUid, "apr01@school.com",
                Role.TEACHER, null, "fp-t");
        mockAuth(teacher);
        TeacherResponse pending = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/self-register",
                HttpMethod.POST,
                authEntity(new TeacherSelfRegisterRequest("App", "Rov", "apr01@school.com", null, "Physics", null)),
                TeacherResponse.class).getBody();
        UUID teacherId = pending.id();

        // Approve as CENTER_ADMIN
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@apr.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        ResponseEntity<TeacherResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/" + teacherId + "/approve",
                HttpMethod.POST, authEntity(), TeacherResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(TeacherStatus.ACTIVE);
    }

    // =========================================================================
    // 8. POST reject — reject a pending teacher
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/teachers/{tid}/reject — returns 200 with INACTIVE status")
    void reject_returns200WithInactiveStatus() {
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("REJ01");

        // Self-register
        UUID teacherUid = UUID.randomUUID();
        AuthPrincipal teacher = new AuthPrincipal(teacherUid, "rej01@school.com",
                Role.TEACHER, null, "fp-t");
        mockAuth(teacher);
        TeacherResponse pending = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/self-register",
                HttpMethod.POST,
                authEntity(new TeacherSelfRegisterRequest("Rej", "Ect", "rej01@school.com", null, "Biology", null)),
                TeacherResponse.class).getBody();
        UUID teacherId = pending.id();

        // Reject as CENTER_ADMIN
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@rej.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        ResponseEntity<TeacherResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/" + teacherId + "/reject",
                HttpMethod.POST,
                authEntity(new com.edutech.center.application.dto.RejectTeacherRequest("Not a good fit")),
                TeacherResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(TeacherStatus.INACTIVE);
    }

    // =========================================================================
    // 9. POST accept-invitation — link stub to userId
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/teachers/accept-invitation — returns 200 and teacher becomes ACTIVE")
    void acceptInvitation_returns200AndActivatesStub() {
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("ACC01");

        // Confirm CSV import to create stub
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@acc.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);
        String uniqueEmail = "ravi.acc01@school.com";
        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar," + uniqueEmail + ",+919876543210,Mathematics,T-001\n";
        restTemplate.exchange("/api/v1/centers/" + centerId + "/teachers/bulk-confirm",
                HttpMethod.POST, csvUploadEntity(csv), BulkImportConfirmResponse.class);

        // Retrieve the invitation token
        String token = teacherRepository.findByCenterId(centerId).stream()
                .filter(t -> t.getEmail().equals(uniqueEmail))
                .map(com.edutech.center.domain.model.Teacher::getInvitationToken)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Stub not found"));

        // Accept invitation (authenticated as a new user who just registered)
        UUID newUserId = UUID.randomUUID();
        AuthPrincipal newTeacher = new AuthPrincipal(newUserId, uniqueEmail, Role.TEACHER, null, "fp-new");
        mockAuth(newTeacher);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/accept-invitation",
                HttpMethod.POST,
                authEntity(new AcceptInvitationRequest(token, newUserId)),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify stub is now ACTIVE with userId linked
        teacherRepository.findByCenterId(centerId).stream()
                .filter(t -> t.getEmail().equals(uniqueEmail))
                .findFirst()
                .ifPresent(t -> {
                    assertThat(t.getUserId()).isEqualTo(newUserId);
                    assertThat(t.getStatus()).isEqualTo(TeacherStatus.ACTIVE);
                });
    }

    @Test
    @DisplayName("POST /centers/{id}/teachers/accept-invitation — invalid token returns 400")
    void acceptInvitation_invalidToken_returns400() {
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("ACC02");
        AuthPrincipal teacher = new AuthPrincipal(UUID.randomUUID(), "t@t.com", Role.TEACHER, null, "fp");
        mockAuth(teacher);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/teachers/accept-invitation",
                HttpMethod.POST,
                authEntity(new AcceptInvitationRequest("no-such-token", UUID.randomUUID())),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // 10. POST /centers/{id}/staff — create staff with profile fields
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/staff — returns 201 with INVITATION_SENT and staff profile fields")
    void createStaff_returns201WithInvitationSentAndProfileFields() {
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("STF01");
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@stf01.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        CreateStaffRequest req = new CreateStaffRequest(
                "Priya", "Sharma", "priya.stf01@school.com", "+919876543210",
                "EMP-101", StaffRoleType.HOD,
                "Head of Mathematics Department",
                "Mathematics,Physics", "Bengaluru",
                "M.Sc Mathematics, B.Ed", 8,
                "Dr. Priya Sharma is an accomplished educator specialising in Mathematics.");

        ResponseEntity<TeacherResponse> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/staff",
                HttpMethod.POST, authEntity(req), TeacherResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TeacherResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.firstName()).isEqualTo("Priya");
        assertThat(body.status()).isEqualTo(TeacherStatus.INVITATION_SENT);
        assertThat(body.roleType()).isEqualTo(StaffRoleType.HOD);
        assertThat(body.designation()).isEqualTo("Head of Mathematics Department");
        assertThat(body.qualification()).isEqualTo("M.Sc Mathematics, B.Ed");
        assertThat(body.yearsOfExperience()).isEqualTo(8);
        assertThat(body.bio()).isNotBlank();
        assertThat(body.employeeId()).isEqualTo("EMP-101");
    }

    // =========================================================================
    // 11. POST /centers/{id}/staff — duplicate email is rejected with 409
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/staff — duplicate email in same center returns 409")
    void createStaff_duplicateEmail_returns409() {
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("STF02");
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@stf02.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        CreateStaffRequest req = new CreateStaffRequest(
                "Amit", "Verma", "amit.stf02@school.com", null,
                null, StaffRoleType.TEACHER, "Senior Teacher",
                "Biology", null, "B.Sc Biology, B.Ed", 3, null);

        // First creation — should succeed
        restTemplate.exchange("/api/v1/centers/" + centerId + "/staff",
                HttpMethod.POST, authEntity(req), TeacherResponse.class);

        // Duplicate — same email, same center
        ResponseEntity<String> dupe = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/staff",
                HttpMethod.POST, authEntity(req), String.class);

        assertThat(dupe.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // =========================================================================
    // 12. PATCH /centers/{id}/staff/{sid} — partial profile update
    // =========================================================================

    @Test
    @DisplayName("PATCH /centers/{id}/staff/{sid} — updates only supplied fields, leaves others unchanged")
    void updateStaff_partialUpdate_appliesOnlyNonNullFields() {
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("STF03");
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@stf03.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        // Create
        CreateStaffRequest create = new CreateStaffRequest(
                "Lakshmi", "Nair", "lakshmi.stf03@school.com", "+911234567890",
                "EMP-202", StaffRoleType.COORDINATOR, "Academic Coordinator",
                "English,History", "Chennai", "MA English, B.Ed", 5, "A dedicated educator.");

        TeacherResponse created = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/staff",
                HttpMethod.POST, authEntity(create), TeacherResponse.class).getBody();
        assertThat(created).isNotNull();

        // Patch — update designation and yearsOfExperience only
        UpdateStaffRequest patch = new UpdateStaffRequest(
                null, null, null,
                null, "Senior Academic Coordinator",
                null, null, null, 6, null);

        ResponseEntity<TeacherResponse> updated = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/staff/" + created.id(),
                HttpMethod.PATCH, authEntity(patch), TeacherResponse.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        TeacherResponse body = updated.getBody();
        assertThat(body).isNotNull();
        assertThat(body.designation()).isEqualTo("Senior Academic Coordinator");
        assertThat(body.yearsOfExperience()).isEqualTo(6);
        // Unchanged fields must be preserved
        assertThat(body.firstName()).isEqualTo("Lakshmi");
        assertThat(body.qualification()).isEqualTo("MA English, B.Ed");
        assertThat(body.roleType()).isEqualTo(StaffRoleType.COORDINATOR);
    }

    // =========================================================================
    // 13. DELETE /centers/{id}/staff/{sid} — deactivate (soft delete)
    // =========================================================================

    @Test
    @DisplayName("DELETE /centers/{id}/staff/{sid} — returns 200 with INACTIVE status")
    void deactivateStaff_returns200WithInactiveStatus() {
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("STF04");
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@stf04.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        CreateStaffRequest create = new CreateStaffRequest(
                "Rahul", "Gupta", "rahul.stf04@school.com", null,
                "EMP-303", StaffRoleType.LAB_ASSISTANT, "Chemistry Lab Assistant",
                "Chemistry", "Mumbai", "B.Sc Chemistry", 2, null);

        TeacherResponse created = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/staff",
                HttpMethod.POST, authEntity(create), TeacherResponse.class).getBody();
        assertThat(created).isNotNull();

        ResponseEntity<TeacherResponse> deactivated = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/staff/" + created.id(),
                HttpMethod.DELETE, authEntity(), TeacherResponse.class);

        assertThat(deactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deactivated.getBody()).isNotNull();
        assertThat(deactivated.getBody().status()).isEqualTo(TeacherStatus.INACTIVE);
    }

    // =========================================================================
    // 14. GET /centers/{id}/staff?roleType=HOD — filter by role type
    // =========================================================================

    @Test
    @DisplayName("GET /centers/{id}/staff?roleType=HOD — returns only HOD staff")
    void listStaff_filterByRoleType_returnsMatchingOnly() {
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("STF05");
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@stf05.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        // Create one HOD and one TEACHER
        restTemplate.exchange("/api/v1/centers/" + centerId + "/staff", HttpMethod.POST,
                authEntity(new CreateStaffRequest("Anita", "Reddy", "anita.stf05@school.com",
                        null, null, StaffRoleType.HOD, "HOD Chemistry",
                        "Chemistry", null, "M.Sc Chemistry", 12, null)),
                TeacherResponse.class);

        restTemplate.exchange("/api/v1/centers/" + centerId + "/staff", HttpMethod.POST,
                authEntity(new CreateStaffRequest("Kiran", "Rao", "kiran.stf05@school.com",
                        null, null, StaffRoleType.TEACHER, "Physics Teacher",
                        "Physics", null, "M.Sc Physics", 4, null)),
                TeacherResponse.class);

        ResponseEntity<TeacherResponse[]> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/staff?roleType=HOD",
                HttpMethod.GET, authEntity(), TeacherResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].roleType()).isEqualTo(StaffRoleType.HOD);
        assertThat(response.getBody()[0].firstName()).isEqualTo("Anita");
    }

    // =========================================================================
    // 15. GET /centers/{id}/staff?status=INVITATION_SENT — filter by status
    // =========================================================================

    @Test
    @DisplayName("GET /centers/{id}/staff?status=INVITATION_SENT — returns only pending-invite staff")
    void listStaff_filterByStatus_returnsMatchingOnly() {
        mockAuth(superAdminPrincipal);
        UUID centerId = createCenter("STF06");
        AuthPrincipal admin = new AuthPrincipal(OWNER_ID, "admin@stf06.com",
                Role.CENTER_ADMIN, centerId, "fp");
        mockAuth(admin);

        // Create two staff via invitation
        restTemplate.exchange("/api/v1/centers/" + centerId + "/staff", HttpMethod.POST,
                authEntity(new CreateStaffRequest("Meena", "Pillai", "meena.stf06@school.com",
                        null, null, StaffRoleType.COUNSELOR, "Career Counselor",
                        null, null, "M.A Psychology", 6, null)),
                TeacherResponse.class);

        restTemplate.exchange("/api/v1/centers/" + centerId + "/staff", HttpMethod.POST,
                authEntity(new CreateStaffRequest("Deepa", "Nambiar", "deepa.stf06@school.com",
                        null, null, StaffRoleType.LIBRARIAN, "Senior Librarian",
                        null, null, "M.Lib Science", 10, null)),
                TeacherResponse.class);

        // All should be INVITATION_SENT since no one accepted yet
        ResponseEntity<TeacherResponse[]> response = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/staff?status=INVITATION_SENT",
                HttpMethod.GET, authEntity(), TeacherResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        for (TeacherResponse r : response.getBody()) {
            assertThat(r.status()).isEqualTo(TeacherStatus.INVITATION_SENT);
        }
    }
}
