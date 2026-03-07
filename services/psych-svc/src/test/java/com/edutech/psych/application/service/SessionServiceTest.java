package com.edutech.psych.application.service;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.application.dto.CompleteSessionRequest;
import com.edutech.psych.application.dto.SessionResponse;
import com.edutech.psych.application.dto.StartSessionRequest;
import com.edutech.psych.application.exception.ProfileNotActiveException;
import com.edutech.psych.application.exception.PsychAccessDeniedException;
import com.edutech.psych.application.exception.PsychProfileNotFoundException;
import com.edutech.psych.application.exception.SessionAlreadyCompletedException;
import com.edutech.psych.application.exception.SessionHistoryNotFoundException;
import com.edutech.psych.domain.model.ProfileStatus;
import com.edutech.psych.domain.model.PsychProfile;
import com.edutech.psych.domain.model.Role;
import com.edutech.psych.domain.model.SessionHistory;
import com.edutech.psych.domain.model.SessionStatus;
import com.edutech.psych.domain.model.SessionType;
import com.edutech.psych.domain.port.out.PsychEventPublisher;
import com.edutech.psych.domain.port.out.PsychProfileRepository;
import com.edutech.psych.domain.port.out.SessionHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SessionService Unit Tests")
class SessionServiceTest {

    private static final UUID STUDENT_ID  = UUID.randomUUID();
    private static final UUID PROFILE_ID  = UUID.randomUUID();
    private static final UUID CENTER_ID   = UUID.randomUUID();
    private static final UUID BATCH_ID    = UUID.randomUUID();

    @Mock PsychProfileRepository profileRepository;
    @Mock SessionHistoryRepository sessionRepository;
    @Mock PsychEventPublisher eventPublisher;
    @InjectMocks SessionService sessionService;

    // ---- helpers ----

    private AuthPrincipal studentPrincipal() {
        return new AuthPrincipal(STUDENT_ID, "student@test.com", Role.STUDENT, null, "fp");
    }

    private AuthPrincipal otherPrincipal() {
        return new AuthPrincipal(UUID.randomUUID(), "other@test.com", Role.STUDENT, null, "fp");
    }

    private PsychProfile activeProfile() {
        PsychProfile profile = mock(PsychProfile.class);
        when(profile.getId()).thenReturn(PROFILE_ID);
        when(profile.getStudentId()).thenReturn(STUDENT_ID);
        when(profile.getCenterId()).thenReturn(CENTER_ID);
        when(profile.getStatus()).thenReturn(ProfileStatus.ACTIVE);
        return profile;
    }

    private StartSessionRequest startRequest() {
        return new StartSessionRequest(SessionType.INITIAL, Instant.now().plusSeconds(3600));
    }

    // ---- startSession tests ----

    @Test
    @DisplayName("startSession_success: ACTIVE profile, enrolled student → IN_PROGRESS session created")
    void startSession_success() {
        PsychProfile profile = activeProfile();
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(sessionRepository.save(any(SessionHistory.class))).thenAnswer(i -> i.getArgument(0));

        SessionResponse response = sessionService.startSession(PROFILE_ID, startRequest(), studentPrincipal());

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(SessionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("startSession_profileNotFound: unknown profileId → throws PsychProfileNotFoundException")
    void startSession_profileNotFound() {
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.startSession(PROFILE_ID, startRequest(), studentPrincipal()))
            .isInstanceOf(PsychProfileNotFoundException.class);

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("startSession_accessDenied: different user → throws PsychAccessDeniedException")
    void startSession_accessDenied() {
        PsychProfile profile = activeProfile();
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> sessionService.startSession(PROFILE_ID, startRequest(), otherPrincipal()))
            .isInstanceOf(PsychAccessDeniedException.class);

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("startSession_profileNotActive: DRAFT profile → throws ProfileNotActiveException")
    void startSession_profileNotActive() {
        PsychProfile profile = mock(PsychProfile.class);
        when(profile.getStudentId()).thenReturn(STUDENT_ID);
        when(profile.getStatus()).thenReturn(ProfileStatus.DRAFT);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> sessionService.startSession(PROFILE_ID, startRequest(), studentPrincipal()))
            .isInstanceOf(ProfileNotActiveException.class);

        verify(sessionRepository, never()).save(any());
    }

    // ---- completeSession tests ----

    @Test
    @DisplayName("completeSession_success: IN_PROGRESS session → COMPLETED, profile traits updated, event published")
    void completeSession_success() {
        // Use real SessionHistory.create so state transitions work
        SessionHistory session = SessionHistory.create(PROFILE_ID, STUDENT_ID, CENTER_ID,
            SessionType.INITIAL, Instant.now().plusSeconds(3600));
        session.start(); // → IN_PROGRESS

        PsychProfile profile = mock(PsychProfile.class);
        when(profile.getId()).thenReturn(PROFILE_ID);
        when(profile.getStudentId()).thenReturn(STUDENT_ID);
        when(profile.getStatus()).thenReturn(ProfileStatus.ACTIVE);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CompleteSessionRequest req = new CompleteSessionRequest(
            0.75, 0.80, 0.65, 0.70, 0.30, "RIA", "Session notes");

        SessionResponse response = sessionService.completeSession(session.getId(), req, studentPrincipal());

        assertThat(response.status()).isEqualTo(SessionStatus.COMPLETED);
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("completeSession_notFound: unknown sessionId → throws SessionHistoryNotFoundException")
    void completeSession_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(sessionRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.completeSession(unknownId,
            new CompleteSessionRequest(0.5, 0.5, 0.5, 0.5, 0.5, "RI", "notes"),
            studentPrincipal()))
            .isInstanceOf(SessionHistoryNotFoundException.class);
    }

    @Test
    @DisplayName("completeSession_alreadyCompleted: COMPLETED session → throws SessionAlreadyCompletedException")
    void completeSession_alreadyCompleted() {
        SessionHistory session = SessionHistory.create(PROFILE_ID, STUDENT_ID, CENTER_ID,
            SessionType.INITIAL, Instant.now().plusSeconds(3600));
        session.start();
        session.complete("already done");  // → COMPLETED

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.completeSession(session.getId(),
            new CompleteSessionRequest(0.5, 0.5, 0.5, 0.5, 0.5, "RI", "notes"),
            studentPrincipal()))
            .isInstanceOf(SessionAlreadyCompletedException.class);
    }
}
