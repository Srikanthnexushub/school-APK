package com.edutech.parent;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.CreateParentProfileRequest;
import com.edutech.parent.application.dto.LinkStudentRequest;
import com.edutech.parent.application.dto.ParentProfileResponse;
import com.edutech.parent.application.dto.StudentLinkResponse;
import com.edutech.parent.domain.model.Role;
import com.edutech.parent.infrastructure.security.JwtTokenValidator;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Integration test for InternalParentController — the canonical service-to-service
 * parent-student link validation endpoint introduced in Fix #321.
 *
 * ROOT CAUSE CONTEXT:
 *   Previously, psych-svc validated parent-student links by calling the user-facing
 *   GET /api/v1/parents/by-student?studentId=X with X-Service-Key. That endpoint
 *   ignores the query param and uses principal.userId() — with X-Service-Key the
 *   synthetic principal is 00000000-..., so it always returned empty and all parents
 *   got 403 on their child's psychometric profile.
 *
 * THIS TEST PERMANENTLY GUARDS AGAINST:
 *   1. The internal endpoint being deleted or renamed (breaking psych-svc calls)
 *   2. The link status check being bypassed (revoked link must return false)
 *   3. Wrong parent userId being accepted (no profile → false, not exception)
 *
 * Uses the same infrastructure pattern as ParentControllerIT:
 *   - Native local Postgres (env vars: POSTGRES_HOST/PORT/PARENT_SVC_DB_NAME/USERNAME/PASSWORD)
 *   - Native local Kafka (KAFKA_BOOTSTRAP_SERVERS)
 *   - JwtTokenValidator @MockBean (no RSA key in test environment)
 *   - Internal calls use X-Service-Key (ServiceKeyAuthFilter handles auth)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InternalParentControllerIT {

    @Autowired
    TestRestTemplate restTemplate;

    @MockBean
    JwtTokenValidator jwtTokenValidator;

    // ── Helpers ────────────────────────────────────────────────────────────────

    private HttpHeaders authHeaders(UUID userId) {
        given(jwtTokenValidator.validate(anyString()))
                .willReturn(Optional.of(new AuthPrincipal(userId, userId + "@test.com", Role.PARENT, null, "fp")));
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth("test-token");
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders serviceKeyHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Service-Key", "test-service-key");
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    /**
     * Creates a parent profile for the given userId and returns its internal profile ID.
     */
    private UUID createParentProfile(UUID userId, String email) {
        CreateParentProfileRequest req = new CreateParentProfileRequest(
                "Test Parent", "+91900000" + (int)(Math.random() * 9999),
                email, null, "Chennai", null, "Tamil Nadu", null, "600001",
                "PARENT", null, null
        );
        ResponseEntity<ParentProfileResponse> r = restTemplate.exchange(
                "/api/v1/parents",
                HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(userId)),
                ParentProfileResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().id();
    }

    /**
     * Links a student to the given parent profile and returns the link response.
     */
    private StudentLinkResponse linkStudent(UUID profileId, UUID studentId, UUID userId) {
        LinkStudentRequest req = new LinkStudentRequest(
                studentId, "Test Student", null,
                null, null, null, null, null, null
        );
        ResponseEntity<StudentLinkResponse> r = restTemplate.exchange(
                "/api/v1/parents/" + profileId + "/students",
                HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(userId)),
                StudentLinkResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody();
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /internal/.../is-linked-to-student — true when ACTIVE link exists")
    void isLinkedToStudent_true_whenActiveLinkExists() {
        UUID parentUserId = UUID.randomUUID();
        UUID studentId    = UUID.randomUUID();

        UUID profileId = createParentProfile(parentUserId, "linked." + parentUserId + "@test.com");
        linkStudent(profileId, studentId, parentUserId);

        ResponseEntity<Boolean> response = restTemplate.exchange(
                "/api/v1/internal/parents/" + parentUserId + "/is-linked-to-student/" + studentId,
                HttpMethod.GET,
                new HttpEntity<>(serviceKeyHeaders()),
                Boolean.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();
    }

    @Test
    @DisplayName("GET /internal/.../is-linked-to-student — false when no profile exists for parentUserId")
    void isLinkedToStudent_false_whenNoParentProfile() {
        UUID unknownParentUserId = UUID.randomUUID();
        UUID studentId           = UUID.randomUUID();

        // No parent profile created for unknownParentUserId

        ResponseEntity<Boolean> response = restTemplate.exchange(
                "/api/v1/internal/parents/" + unknownParentUserId + "/is-linked-to-student/" + studentId,
                HttpMethod.GET,
                new HttpEntity<>(serviceKeyHeaders()),
                Boolean.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isFalse();
    }

    @Test
    @DisplayName("GET /internal/.../is-linked-to-student — false when link is REVOKED (status check)")
    void isLinkedToStudent_false_whenLinkIsRevoked() {
        UUID parentUserId = UUID.randomUUID();
        UUID studentId    = UUID.randomUUID();

        UUID profileId = createParentProfile(parentUserId, "revoked." + parentUserId + "@test.com");
        StudentLinkResponse link = linkStudent(profileId, studentId, parentUserId);

        // Revoke the link
        restTemplate.exchange(
                "/api/v1/parents/" + profileId + "/students/" + link.id() + "/revoke",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(parentUserId)),
                Object.class
        );

        ResponseEntity<Boolean> response = restTemplate.exchange(
                "/api/v1/internal/parents/" + parentUserId + "/is-linked-to-student/" + studentId,
                HttpMethod.GET,
                new HttpEntity<>(serviceKeyHeaders()),
                Boolean.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isFalse();
    }

    @Test
    @DisplayName("GET /internal/.../is-linked-to-student — false when different student queried")
    void isLinkedToStudent_false_whenDifferentStudentQueried() {
        UUID parentUserId   = UUID.randomUUID();
        UUID linkedStudent  = UUID.randomUUID();
        UUID otherStudent   = UUID.randomUUID();

        UUID profileId = createParentProfile(parentUserId, "other." + parentUserId + "@test.com");
        linkStudent(profileId, linkedStudent, parentUserId);

        // Query with a DIFFERENT student who was never linked
        ResponseEntity<Boolean> response = restTemplate.exchange(
                "/api/v1/internal/parents/" + parentUserId + "/is-linked-to-student/" + otherStudent,
                HttpMethod.GET,
                new HttpEntity<>(serviceKeyHeaders()),
                Boolean.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isFalse();
    }
}
