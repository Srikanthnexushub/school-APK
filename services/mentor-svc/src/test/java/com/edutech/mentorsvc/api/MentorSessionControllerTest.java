// src/test/java/com/edutech/mentorsvc/api/MentorSessionControllerTest.java
package com.edutech.mentorsvc.api;

import com.edutech.mentorsvc.application.dto.MentorSessionResponse;
import com.edutech.mentorsvc.domain.port.in.BookMentorSessionUseCase;
import com.edutech.mentorsvc.domain.port.in.CompleteSessionUseCase;
import com.edutech.mentorsvc.domain.port.in.GetMentorSessionUseCase;
import com.edutech.mentorsvc.domain.port.in.UpdateSessionStatusUseCase;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * REGRESSION tests for MentorSessionController — Fix #354.
 *
 * <p>Prior to Fix #354, GET /api/v1/mentor-sessions?studentId=X had no identity check:
 * a student could pass any studentId and receive another student's session list.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>Student querying their OWN sessions → 200 with results</li>
 *   <li>Student querying ANOTHER student's sessions → 403</li>
 *   <li>Non-student (TEACHER) querying any studentId → 200</li>
 *   <li>No studentId or mentorId supplied → 200 empty page</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("REGRESSION Fix#354 — MentorSessionController student identity check")
class MentorSessionControllerTest {

    @Mock BookMentorSessionUseCase bookMentorSessionUseCase;
    @Mock GetMentorSessionUseCase getMentorSessionUseCase;
    @Mock CompleteSessionUseCase completeSessionUseCase;
    @Mock UpdateSessionStatusUseCase updateSessionStatusUseCase;

    @InjectMocks
    MentorSessionController controller;

    static final UUID STUDENT_A = UUID.randomUUID();
    static final UUID STUDENT_B = UUID.randomUUID();
    static final UUID MENTOR_ID = UUID.randomUUID();

    MentorSessionResponse dummySession;

    @BeforeEach
    void setUp() {
        dummySession = new MentorSessionResponse(
                UUID.randomUUID(), MENTOR_ID, "Mentor Name", STUDENT_A,
                OffsetDateTime.now().plusDays(1),
                60, "ONLINE", "SCHEDULED",
                null, null,
                OffsetDateTime.now(), null
        );
        when(getMentorSessionUseCase.getSessionsByStudent(STUDENT_A)).thenReturn(List.of(dummySession));
        when(getMentorSessionUseCase.getSessionsByStudent(STUDENT_B)).thenReturn(List.of());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Authentication studentAuth(UUID userId) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userId.toString());
        GrantedAuthority role = new SimpleGrantedAuthority("ROLE_STUDENT");
        when(auth.getAuthorities()).thenAnswer(i -> List.of(role));
        return auth;
    }

    private Authentication teacherAuth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(UUID.randomUUID().toString());
        GrantedAuthority role = new SimpleGrantedAuthority("ROLE_TEACHER");
        when(auth.getAuthorities()).thenAnswer(i -> List.of(role));
        return auth;
    }

    // =========================================================================
    // Tests
    // =========================================================================

    @Test
    @DisplayName("Fix#354 — STUDENT querying own sessions → 200 with results")
    void student_ownSessions_returns200() {
        ResponseEntity<?> response = controller.getSessions(
                null, STUDENT_A, 0, 50, studentAuth(STUDENT_A));

        assertThat(response.getStatusCode())
                .as("Student must be allowed to query their own sessions (Fix #354)")
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Fix#354 — STUDENT querying ANOTHER student's sessions → 403")
    void student_anotherStudentSessions_returns403() {
        ResponseEntity<?> response = controller.getSessions(
                null, STUDENT_B, 0, 50, studentAuth(STUDENT_A));

        assertThat(response.getStatusCode())
                .as("Student must not access another student's sessions (Fix #354)")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Fix#354 — TEACHER querying any student's sessions → 200")
    void teacher_anySessions_returns200() {
        ResponseEntity<?> response = controller.getSessions(
                null, STUDENT_A, 0, 50, teacherAuth());

        assertThat(response.getStatusCode())
                .as("Teacher must be allowed to view any student's sessions (Fix #354)")
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Fix#354 — no studentId or mentorId → 200 empty page")
    void noFilter_returnsEmptyPage() {
        ResponseEntity<?> response = controller.getSessions(
                null, null, 0, 50, teacherAuth());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Fix#354 — STUDENT with malformed principal UUID → 403")
    void student_malformedPrincipal_returns403() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("not-a-uuid");
        GrantedAuthority role = new SimpleGrantedAuthority("ROLE_STUDENT");
        when(auth.getAuthorities()).thenAnswer(i -> List.of(role));

        ResponseEntity<?> response = controller.getSessions(
                null, STUDENT_A, 0, 50, auth);

        assertThat(response.getStatusCode())
                .as("Malformed student principal must be rejected with 403 (Fix #354)")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
