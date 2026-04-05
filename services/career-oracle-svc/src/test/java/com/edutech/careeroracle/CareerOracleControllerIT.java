package com.edutech.careeroracle;

import com.edutech.careeroracle.application.dto.CareerProfileResponse;
import com.edutech.careeroracle.application.dto.CareerRecommendationResponse;
import com.edutech.careeroracle.application.dto.CollegePredictionResponse;
import com.edutech.careeroracle.application.dto.CreateCareerProfileRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for career-oracle-svc controllers.
 *
 * FIX #323 — guards against two bug classes:
 *
 * BUG A (CollegePrediction no-profile crash):
 *   CollegePredictionService.predictColleges() previously threw CareerProfileNotFoundException
 *   (404) when no career profile existed for the student — unlike CareerRecommendationService
 *   which auto-creates a default profile. This inconsistency caused POST /predict to fail
 *   for any student who hadn't first generated career recommendations.
 *   GUARD: test_collegePrediction_autoCreatesProfileAndReturns200 must not get 404.
 *
 * BUG B (no controller regression coverage):
 *   All 3 career-oracle-svc controllers had zero integration tests.
 *   A regression like Fix #321 (wrong endpoint) or Fix #322 (TODO placeholder)
 *   would go undetected. These tests provide permanent coverage.
 *
 * AUTH MODEL:
 *   career-oracle-svc uses GatewayHeaderAuthFilter — no JWT needed.
 *   Tests pass X-User-Id and X-User-Role headers directly, simulating
 *   what api-gateway injects after JWT validation.
 *
 * INFRA:
 *   Native local Postgres (env vars: POSTGRES_HOST/PORT/CAREER_ORACLE_DB_NAME/USER/PASSWORD).
 *   Kafka listener.auto-startup=false — Kafka not required.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CareerOracleControllerIT {

    @Autowired
    TestRestTemplate restTemplate;

    // ── Header helpers ─────────────────────────────────────────────────────────

    private HttpHeaders studentHeaders(UUID studentId) {
        HttpHeaders h = new HttpHeaders();
        h.set("X-User-Id", studentId.toString());
        h.set("X-User-Role", "STUDENT");
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-User-Id", UUID.randomUUID().toString());
        h.set("X-User-Role", "INSTITUTION_ADMIN");
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    // ── CareerProfile tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /career-profiles/by-student/{id} — student gets own profile → 200")
    void careerProfile_studentGetsOwnProfile_200() {
        UUID studentId = UUID.randomUUID();

        // Admin creates a profile for the student first
        CreateCareerProfileRequest req = new CreateCareerProfileRequest(
                studentId, UUID.randomUUID(), "SCIENCE", 11,
                BigDecimal.valueOf(65), null
        );
        ResponseEntity<CareerProfileResponse> created = restTemplate.exchange(
                "/api/v1/career-profiles",
                HttpMethod.POST,
                new HttpEntity<>(req, adminHeaders()),
                CareerProfileResponse.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Student reads own profile
        ResponseEntity<CareerProfileResponse> response = restTemplate.exchange(
                "/api/v1/career-profiles/by-student/" + studentId,
                HttpMethod.GET,
                new HttpEntity<>(studentHeaders(studentId)),
                CareerProfileResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().studentId()).isEqualTo(studentId);
    }

    @Test
    @DisplayName("GET /career-profiles/by-student/{id} — student accesses other student's profile → 403")
    void careerProfile_studentAccessesOtherStudent_403() {
        UUID ownerStudentId = UUID.randomUUID();
        UUID attackerStudentId = UUID.randomUUID();

        // Create profile for ownerStudent
        CreateCareerProfileRequest req = new CreateCareerProfileRequest(
                ownerStudentId, UUID.randomUUID(), "COMMERCE", 12,
                BigDecimal.valueOf(70), null
        );
        restTemplate.exchange(
                "/api/v1/career-profiles",
                HttpMethod.POST,
                new HttpEntity<>(req, adminHeaders()),
                CareerProfileResponse.class
        );

        // attackerStudent tries to read ownerStudent's profile
        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/career-profiles/by-student/" + ownerStudentId,
                HttpMethod.GET,
                new HttpEntity<>(studentHeaders(attackerStudentId)),
                Object.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /career-profiles/by-student/{id} — profile does not exist → 404")
    void careerProfile_notFound_404() {
        UUID noProfileId = UUID.randomUUID();

        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/career-profiles/by-student/" + noProfileId,
                HttpMethod.GET,
                new HttpEntity<>(studentHeaders(noProfileId)),
                Object.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── CareerRecommendation tests ─────────────────────────────────────────────

    @Test
    @DisplayName("POST /career-recommendations/students/{id}/generate — auto-creates profile and returns recs → 200")
    void recommendations_autoCreatesProfileAndReturns200() {
        // Regression: Fix #323 Bug A (this endpoint must NOT throw 404 even with no prior profile)
        UUID studentId = UUID.randomUUID();

        ResponseEntity<List<CareerRecommendationResponse>> response = restTemplate.exchange(
                "/api/v1/career-recommendations/students/" + studentId + "/generate",
                HttpMethod.POST,
                new HttpEntity<>(studentHeaders(studentId)),
                new ParameterizedTypeReference<List<CareerRecommendationResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("POST /career-recommendations/.../generate — student generates own → 200; wrong student → 403")
    void recommendations_generate_studentScopeEnforced() {
        UUID studentId = UUID.randomUUID();
        UUID otherStudentId = UUID.randomUUID();

        // Own ID → 200
        ResponseEntity<List<CareerRecommendationResponse>> ok = restTemplate.exchange(
                "/api/v1/career-recommendations/students/" + studentId + "/generate",
                HttpMethod.POST,
                new HttpEntity<>(studentHeaders(studentId)),
                new ParameterizedTypeReference<List<CareerRecommendationResponse>>() {}
        );
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Other student's ID → 403
        ResponseEntity<Object> forbidden = restTemplate.exchange(
                "/api/v1/career-recommendations/students/" + otherStudentId + "/generate",
                HttpMethod.POST,
                new HttpEntity<>(studentHeaders(studentId)),
                Object.class
        );
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── CollegePrediction tests ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /college-predictions/students/{id}/predict — auto-creates profile and returns predictions → 200")
    void collegePrediction_autoCreatesProfileAndReturns200() {
        // Regression: Fix #323 Bug A — this used to throw 404 when no career profile existed.
        // CollegePredictionService now uses autoCreateProfile() instead of orElseThrow().
        UUID studentId = UUID.randomUUID();

        ResponseEntity<List<CollegePredictionResponse>> response = restTemplate.exchange(
                "/api/v1/college-predictions/students/" + studentId + "/predict",
                HttpMethod.POST,
                new HttpEntity<>(studentHeaders(studentId)),
                new ParameterizedTypeReference<List<CollegePredictionResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(3); // TIER_1, TIER_2, TIER_3
    }

    @Test
    @DisplayName("POST /college-predictions/.../predict — wrong student → 403 (student scope enforced)")
    void collegePrediction_wrongStudent_403() {
        UUID studentId = UUID.randomUUID();
        UUID victimId = UUID.randomUUID();

        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/college-predictions/students/" + victimId + "/predict",
                HttpMethod.POST,
                new HttpEntity<>(studentHeaders(studentId)),
                Object.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /college-predictions/students/{id} — admin can view any student's predictions → 200")
    void collegePrediction_adminViewsAnyStudent_200() {
        UUID studentId = UUID.randomUUID();

        // First generate predictions (auto-creates profile)
        restTemplate.exchange(
                "/api/v1/college-predictions/students/" + studentId + "/predict",
                HttpMethod.POST,
                new HttpEntity<>(adminHeaders()),
                Object.class
        );

        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/college-predictions/students/" + studentId,
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                Object.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
