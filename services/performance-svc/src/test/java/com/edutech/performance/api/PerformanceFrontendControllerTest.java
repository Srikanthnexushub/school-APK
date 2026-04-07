// src/test/java/com/edutech/performance/api/PerformanceFrontendControllerTest.java
package com.edutech.performance.api;

import com.edutech.performance.domain.model.ReadinessScore;
import com.edutech.performance.domain.port.out.AiDropoutRiskPort;
import com.edutech.performance.domain.port.out.ReadinessScoreRepository;
import com.edutech.performance.domain.port.out.SubjectMasteryRepository;
import com.edutech.performance.domain.port.out.WeakAreaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * REGRESSION tests for PerformanceFrontendController — Fix #353.
 *
 * <p>Prior to Fix #353, all four performance endpoints had NO authentication check.
 * Any authenticated user could read any other student's readiness / mastery /
 * weak-areas / dropout-risk data by changing the {studentId} path variable.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>A student calling with their OWN studentId → 200</li>
 *   <li>A student calling with ANOTHER student's id → 403</li>
 *   <li>A non-student (TEACHER/ADMIN) calling with any studentId → 200</li>
 *   <li>Null authentication → 403</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("REGRESSION Fix#353 — PerformanceFrontendController access control")
class PerformanceFrontendControllerTest {

    @Mock ReadinessScoreRepository readinessScoreRepository;
    @Mock SubjectMasteryRepository subjectMasteryRepository;
    @Mock WeakAreaRepository weakAreaRepository;
    @Mock AiDropoutRiskPort aiDropoutRiskPort;

    @InjectMocks
    PerformanceFrontendController controller;

    static final UUID STUDENT_A = UUID.randomUUID();
    static final UUID STUDENT_B = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // readinessRepo returns empty by default — enough for 200 path
        when(readinessScoreRepository.findLatestByStudentId(any())).thenReturn(Optional.empty());
        when(subjectMasteryRepository.findByStudentId(any())).thenReturn(List.of());
        when(weakAreaRepository.findByStudentIdOrderByMasteryAsc(any(), eq(50))).thenReturn(List.of());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Builds a mock Authentication for a STUDENT whose principal is their userId. */
    private Authentication studentAuth(UUID userId) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userId.toString());
        GrantedAuthority role = new SimpleGrantedAuthority("ROLE_STUDENT");
        when(auth.getAuthorities()).thenAnswer(i -> List.of(role));
        return auth;
    }

    /** Builds a mock Authentication for a TEACHER (non-student). */
    private Authentication teacherAuth(UUID userId) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userId.toString());
        GrantedAuthority role = new SimpleGrantedAuthority("ROLE_TEACHER");
        when(auth.getAuthorities()).thenAnswer(i -> List.of(role));
        return auth;
    }

    // =========================================================================
    // /readiness — Fix #353
    // =========================================================================

    @Test
    @DisplayName("Fix#353 readiness — STUDENT accessing OWN data → 200")
    void readiness_student_ownId_returns200() {
        ResponseEntity<?> response = controller.getLatestReadiness(STUDENT_A, studentAuth(STUDENT_A));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Fix#353 readiness — STUDENT accessing ANOTHER student's data → 403")
    void readiness_student_otherId_returns403() {
        ResponseEntity<?> response = controller.getLatestReadiness(STUDENT_B, studentAuth(STUDENT_A));
        assertThat(response.getStatusCode())
                .as("Student must not access another student's readiness (Fix #353)")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Fix#353 readiness — TEACHER accessing any student → 200")
    void readiness_teacher_anyId_returns200() {
        ResponseEntity<?> response = controller.getLatestReadiness(STUDENT_A, teacherAuth(UUID.randomUUID()));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Fix#353 readiness — null auth → 403")
    void readiness_nullAuth_returns403() {
        ResponseEntity<?> response = controller.getLatestReadiness(STUDENT_A, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // /mastery — Fix #353
    // =========================================================================

    @Test
    @DisplayName("Fix#353 mastery — STUDENT accessing OWN data → 200")
    void mastery_student_ownId_returns200() {
        ResponseEntity<?> response = controller.getSubjectMastery(STUDENT_A, studentAuth(STUDENT_A));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Fix#353 mastery — STUDENT accessing ANOTHER student's data → 403")
    void mastery_student_otherId_returns403() {
        ResponseEntity<?> response = controller.getSubjectMastery(STUDENT_B, studentAuth(STUDENT_A));
        assertThat(response.getStatusCode())
                .as("Student must not access another student's mastery (Fix #353)")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // /weak-areas — Fix #353
    // =========================================================================

    @Test
    @DisplayName("Fix#353 weak-areas — STUDENT accessing OWN data → 200")
    void weakAreas_student_ownId_returns200() {
        ResponseEntity<?> response = controller.getWeakAreas(STUDENT_A, studentAuth(STUDENT_A));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Fix#353 weak-areas — STUDENT accessing ANOTHER student's data → 403")
    void weakAreas_student_otherId_returns403() {
        ResponseEntity<?> response = controller.getWeakAreas(STUDENT_B, studentAuth(STUDENT_A));
        assertThat(response.getStatusCode())
                .as("Student must not access another student's weak areas (Fix #353)")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // /dropout-risk — Fix #353
    // =========================================================================

    @Test
    @DisplayName("Fix#353 dropout-risk — STUDENT accessing OWN data → 200")
    void dropoutRisk_student_ownId_returns200() {
        ResponseEntity<?> response = controller.getDropoutRisk(STUDENT_A, studentAuth(STUDENT_A));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Fix#353 dropout-risk — STUDENT accessing ANOTHER student's data → 403")
    void dropoutRisk_student_otherId_returns403() {
        ResponseEntity<?> response = controller.getDropoutRisk(STUDENT_B, studentAuth(STUDENT_A));
        assertThat(response.getStatusCode())
                .as("Student must not access another student's dropout risk (Fix #353)")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Fix#353 dropout-risk — TEACHER accessing any student → 200")
    void dropoutRisk_teacher_anyId_returns200() {
        ResponseEntity<?> response = controller.getDropoutRisk(STUDENT_A, teacherAuth(UUID.randomUUID()));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
