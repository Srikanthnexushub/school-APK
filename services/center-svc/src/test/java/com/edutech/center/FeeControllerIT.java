package com.edutech.center;

import com.edutech.center.application.dto.AssignFeeRequest;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchFeeAssignmentResponse;
import com.edutech.center.application.dto.BatchResponse;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.CreateBatchRequest;
import com.edutech.center.application.dto.CreateCenterRequest;
import com.edutech.center.application.dto.CreateFeeStructureRequest;
import com.edutech.center.application.dto.FeeStructureResponse;
import com.edutech.center.domain.model.FeeFrequency;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HTTP-level integration tests for fee structures and batch fee assignments.
 *
 * <p>Covers {@link com.edutech.center.api.FeeController} and
 * {@link com.edutech.center.api.BatchFeeController}.
 *
 * <p>Test coverage (9 tests):
 * <ul>
 *   <li>POST /centers/{id}/fees — CENTER_ADMIN creates MONTHLY fee structure → 201</li>
 *   <li>POST /centers/{id}/fees — STUDENT role → 403</li>
 *   <li>GET  /centers/{id}/fees — lists fee structures including newly created one</li>
 *   <li>POST /centers/{id}/batches/{bId}/fees — CENTER_ADMIN assigns fee to batch → 201</li>
 *   <li>POST /centers/{id}/batches/{bId}/fees — duplicate assignment rejected → 409</li>
 *   <li>GET  /centers/{id}/batches/{bId}/fees — returns assignment list for batch</li>
 *   <li>DELETE /centers/{id}/batches/{bId}/fees/{assignmentId} — removes assignment → 204</li>
 *   <li>GET  /centers/{id}/batches/{bId}/fees — empty after removal</li>
 *   <li>POST /centers/{id}/fees — ANNUAL fee with lateFeeAmount persisted correctly</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("center-svc — Fee Structures & Batch Fee Assignment Integration Tests")
class FeeControllerIT {

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

    static final UUID SUPER_ADMIN_ID  = UUID.randomUUID();
    static final UUID CENTER_ADMIN_ID = UUID.randomUUID();
    static final UUID STUDENT_ID      = UUID.randomUUID();
    static final UUID OWNER_ID        = UUID.randomUUID();
    static final String FAKE_TOKEN    = "test-token";

    AuthPrincipal superAdminPrincipal;
    AuthPrincipal centerAdminPrincipal;
    AuthPrincipal studentPrincipal;

    UUID centerId;
    UUID batchId;

    @BeforeEach
    void setUp() {
        superAdminPrincipal = new AuthPrincipal(SUPER_ADMIN_ID, "sa@test.com", Role.SUPER_ADMIN, null, "fp-sa");
        mockAuth(superAdminPrincipal);

        centerId = createCenter("FEE" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        centerAdminPrincipal = new AuthPrincipal(CENTER_ADMIN_ID, "ca@test.com", Role.CENTER_ADMIN, centerId, "fp-ca");
        studentPrincipal     = new AuthPrincipal(STUDENT_ID,     "st@test.com", Role.STUDENT,      centerId, "fp-st");

        mockAuth(centerAdminPrincipal);
        batchId = createBatch("Physics Batch", "PHY" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());

    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void mockAuth(AuthPrincipal p) {
        when(jwtTokenValidator.validate(anyString())).thenReturn(Optional.of(p));
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
                "1 Main St", "Bangalore", "Karnataka", "560001",
                "9876543210", code.toLowerCase() + "@test.com",
                null, null, OWNER_ID, null);
        mockAuth(superAdminPrincipal);
        ResponseEntity<CenterResponse> r = restTemplate.exchange(
                "/api/v1/centers", HttpMethod.POST, authEntity(req), CenterResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().id();
    }

    private UUID createBatch(String name, String code) {
        CreateBatchRequest req = new CreateBatchRequest(
                name, code, "Physics", null, 40,
                LocalDate.now(), LocalDate.now().plusMonths(6));
        ResponseEntity<BatchResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches",
                HttpMethod.POST, authEntity(req), BatchResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().id();
    }

    private UUID createFeeStructure(String name, FeeFrequency freq, BigDecimal amount) {
        CreateFeeStructureRequest req = new CreateFeeStructureRequest(
                name, "Fee structure for " + name,
                amount, "INR", freq, 5, null);
        ResponseEntity<FeeStructureResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/fees",
                HttpMethod.POST, authEntity(req), FeeStructureResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().id();
    }

    // =========================================================================
    // Test 1: CENTER_ADMIN creates MONTHLY fee structure → 201
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/fees — CENTER_ADMIN creates MONTHLY fee → 201, fields persisted")
    void createFeeStructure_centerAdmin_succeeds() {
        mockAuth(centerAdminPrincipal);

        CreateFeeStructureRequest req = new CreateFeeStructureRequest(
                "Monthly Tuition", "Standard monthly fee",
                new BigDecimal("4500.00"), "INR",
                FeeFrequency.MONTHLY, 5,
                new BigDecimal("200.00"));

        ResponseEntity<FeeStructureResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/fees",
                HttpMethod.POST, authEntity(req), FeeStructureResponse.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        FeeStructureResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.name()).isEqualTo("Monthly Tuition");
        assertThat(body.amount()).isEqualByComparingTo(new BigDecimal("4500.00"));
        assertThat(body.frequency()).isEqualTo(FeeFrequency.MONTHLY);
        assertThat(body.dueDay()).isEqualTo(5);
        assertThat(body.lateFeeAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(body.centerId()).isEqualTo(centerId);
    }

    // =========================================================================
    // Test 2: STUDENT role → 403
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/fees — STUDENT role → 403 Forbidden")
    void createFeeStructure_student_forbidden() {
        mockAuth(studentPrincipal);

        CreateFeeStructureRequest req = new CreateFeeStructureRequest(
                "Hack Fee", null, new BigDecimal("1.00"), "INR",
                FeeFrequency.MONTHLY, 1, null);

        ResponseEntity<String> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/fees",
                HttpMethod.POST, authEntity(req), String.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // Test 3: GET /centers/{id}/fees — lists fee structures
    // =========================================================================

    @Test
    @DisplayName("GET /centers/{id}/fees — returns list containing newly created fee structure")
    void listFeeStructures_containsCreated() {
        mockAuth(centerAdminPrincipal);
        String name = "Lab Fee " + UUID.randomUUID();
        createFeeStructure(name, FeeFrequency.MONTHLY, new BigDecimal("500.00"));

        ResponseEntity<List<FeeStructureResponse>> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/fees",
                HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<FeeStructureResponse>>() {});

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> names = r.getBody().stream().map(FeeStructureResponse::name).toList();
        assertThat(names).contains(name);
    }

    // =========================================================================
    // Test 4: CENTER_ADMIN assigns fee to batch → 201
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/batches/{bId}/fees — CENTER_ADMIN assigns fee → 201")
    void assignFeeToBatch_centerAdmin_succeeds() {
        mockAuth(centerAdminPrincipal);
        UUID feeId = createFeeStructure("Term Fee", FeeFrequency.QUARTERLY, new BigDecimal("12000.00"));

        AssignFeeRequest req = new AssignFeeRequest(feeId, LocalDate.now(), null);

        ResponseEntity<BatchFeeAssignmentResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches/" + batchId + "/fees",
                HttpMethod.POST, authEntity(req), BatchFeeAssignmentResponse.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BatchFeeAssignmentResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.batchId()).isEqualTo(batchId);
        assertThat(body.feeStructureId()).isEqualTo(feeId);
        assertThat(body.effectiveFrom()).isEqualTo(LocalDate.now());
    }

    // =========================================================================
    // Test 5: Duplicate assignment → 409
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/batches/{bId}/fees — duplicate assignment → 409 Conflict")
    void assignFeeToBatch_duplicate_returns409() {
        mockAuth(centerAdminPrincipal);
        UUID feeId = createFeeStructure("Dup Fee " + UUID.randomUUID(), FeeFrequency.MONTHLY, new BigDecimal("3000.00"));
        LocalDate from = LocalDate.now();

        AssignFeeRequest req = new AssignFeeRequest(feeId, from, null);
        // First assignment
        restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches/" + batchId + "/fees",
                HttpMethod.POST, authEntity(req), BatchFeeAssignmentResponse.class);

        // Duplicate
        ResponseEntity<String> dup = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches/" + batchId + "/fees",
                HttpMethod.POST, authEntity(req), String.class);

        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // =========================================================================
    // Test 6: GET /batches/{bId}/fees — returns assignment list
    // =========================================================================

    @Test
    @DisplayName("GET /centers/{id}/batches/{bId}/fees — returns list of assignments for batch")
    void listBatchFees_returnsAssignment() {
        mockAuth(centerAdminPrincipal);
        UUID feeId = createFeeStructure("Annual Exam Fee " + UUID.randomUUID(), FeeFrequency.ANNUAL, new BigDecimal("8000.00"));

        AssignFeeRequest req = new AssignFeeRequest(feeId, LocalDate.now(), null);
        restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches/" + batchId + "/fees",
                HttpMethod.POST, authEntity(req), BatchFeeAssignmentResponse.class);

        ResponseEntity<List<BatchFeeAssignmentResponse>> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches/" + batchId + "/fees",
                HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<BatchFeeAssignmentResponse>>() {});

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<UUID> feeIds = r.getBody().stream().map(BatchFeeAssignmentResponse::feeStructureId).toList();
        assertThat(feeIds).contains(feeId);
    }

    // =========================================================================
    // Test 7: DELETE assignment → 204
    // =========================================================================

    @Test
    @DisplayName("DELETE /batches/{bId}/fees/{assignmentId} — removes assignment → 204")
    void removeBatchFeeAssignment_succeeds() {
        mockAuth(centerAdminPrincipal);
        UUID feeId = createFeeStructure("Removable Fee " + UUID.randomUUID(), FeeFrequency.MONTHLY, new BigDecimal("2000.00"));

        AssignFeeRequest req = new AssignFeeRequest(feeId, LocalDate.now(), null);
        ResponseEntity<BatchFeeAssignmentResponse> created = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches/" + batchId + "/fees",
                HttpMethod.POST, authEntity(req), BatchFeeAssignmentResponse.class);
        UUID assignmentId = created.getBody().id();

        ResponseEntity<Void> del = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches/" + batchId + "/fees/" + assignmentId,
                HttpMethod.DELETE, authEntity(), Void.class);

        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // =========================================================================
    // Test 8: GET after removal → empty for that fee
    // =========================================================================

    @Test
    @DisplayName("GET /batches/{bId}/fees — assignment absent after removal")
    void listBatchFees_emptyAfterRemoval() {
        mockAuth(centerAdminPrincipal);
        UUID feeId = createFeeStructure("Temp Fee " + UUID.randomUUID(), FeeFrequency.MONTHLY, new BigDecimal("1000.00"));

        AssignFeeRequest req = new AssignFeeRequest(feeId, LocalDate.now(), null);
        ResponseEntity<BatchFeeAssignmentResponse> created = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches/" + batchId + "/fees",
                HttpMethod.POST, authEntity(req), BatchFeeAssignmentResponse.class);
        UUID assignmentId = created.getBody().id();

        restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches/" + batchId + "/fees/" + assignmentId,
                HttpMethod.DELETE, authEntity(), Void.class);

        ResponseEntity<List<BatchFeeAssignmentResponse>> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/batches/" + batchId + "/fees",
                HttpMethod.GET, authEntity(),
                new ParameterizedTypeReference<List<BatchFeeAssignmentResponse>>() {});

        List<UUID> remaining = r.getBody().stream().map(BatchFeeAssignmentResponse::feeStructureId).toList();
        assertThat(remaining).doesNotContain(feeId);
    }

    // =========================================================================
    // Test 9: ANNUAL fee with lateFeeAmount → persisted correctly
    // =========================================================================

    @Test
    @DisplayName("POST /centers/{id}/fees — ANNUAL fee with lateFeeAmount → 201, all fields persisted")
    void createFeeStructure_annual_withLateFee_succeeds() {
        mockAuth(centerAdminPrincipal);

        CreateFeeStructureRequest req = new CreateFeeStructureRequest(
                "Annual Registration", "One-time annual registration charge",
                new BigDecimal("15000.00"), "INR",
                FeeFrequency.ANNUAL, 1,
                new BigDecimal("500.00"));

        ResponseEntity<FeeStructureResponse> r = restTemplate.exchange(
                "/api/v1/centers/" + centerId + "/fees",
                HttpMethod.POST, authEntity(req), FeeStructureResponse.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        FeeStructureResponse body = r.getBody();
        assertThat(body.frequency()).isEqualTo(FeeFrequency.ANNUAL);
        assertThat(body.amount()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(body.lateFeeAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(body.dueDay()).isEqualTo(1);
    }
}
