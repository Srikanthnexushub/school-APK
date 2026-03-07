package com.edutech.mentorsvc.application.service;

import com.edutech.mentorsvc.application.dto.BookSessionRequest;
import com.edutech.mentorsvc.application.dto.MentorSessionResponse;
import com.edutech.mentorsvc.application.exception.MentorNotFoundException;
import com.edutech.mentorsvc.application.exception.SessionAlreadyBookedException;
import com.edutech.mentorsvc.domain.model.MentorProfile;
import com.edutech.mentorsvc.domain.model.MentorSession;
import com.edutech.mentorsvc.domain.model.SessionMode;
import com.edutech.mentorsvc.domain.model.SessionStatus;
import com.edutech.mentorsvc.domain.port.out.MentorEventPublisher;
import com.edutech.mentorsvc.domain.port.out.MentorProfileRepository;
import com.edutech.mentorsvc.domain.port.out.MentorSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MentorSessionServiceTest {

    @Mock
    private MentorSessionRepository mentorSessionRepository;

    @Mock
    private MentorProfileRepository mentorProfileRepository;

    @Mock
    private MentorEventPublisher mentorEventPublisher;

    private MentorSessionService mentorSessionService;

    private MentorProfile availableMentor;
    private UUID mentorId;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        mentorSessionService = new MentorSessionService(
                mentorSessionRepository, mentorProfileRepository, mentorEventPublisher);

        mentorId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        availableMentor = MentorProfile.create(
                UUID.randomUUID(),
                "Dr. Kapil Dev",
                "kapil.dev@edutech.com",
                "JEE Advanced expert",
                "JEE",
                15,
                new BigDecimal("2500.00")
        );
    }

    @Test
    void bookSession_success() {
        // Given
        BookSessionRequest request = new BookSessionRequest(
                mentorId,
                studentId,
                OffsetDateTime.now().plusDays(2),
                60,
                "ONLINE",
                "https://meet.edutech.com/session-abc",
                "Focus on Mechanics chapter"
        );

        MentorSession session = MentorSession.create(
                availableMentor,
                studentId,
                request.scheduledAt(),
                request.durationMinutes(),
                SessionMode.ONLINE,
                request.meetingLink(),
                request.notes()
        );

        when(mentorProfileRepository.findById(mentorId)).thenReturn(Optional.of(availableMentor));
        when(mentorSessionRepository.save(any(MentorSession.class))).thenReturn(session);

        // When
        MentorSessionResponse response = mentorSessionService.bookSession(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(SessionStatus.SCHEDULED.name());
        assertThat(response.sessionMode()).isEqualTo(SessionMode.ONLINE.name());
        verify(mentorSessionRepository, times(1)).save(any(MentorSession.class));
        verify(mentorEventPublisher, times(1)).publishSessionBooked(any());
    }

    @Test
    void bookSession_mentorNotFound() {
        // Given
        BookSessionRequest request = new BookSessionRequest(
                mentorId,
                studentId,
                OffsetDateTime.now().plusDays(1),
                45,
                "ONLINE",
                null,
                null
        );

        when(mentorProfileRepository.findById(mentorId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> mentorSessionService.bookSession(request))
                .isInstanceOf(MentorNotFoundException.class)
                .hasMessageContaining(mentorId.toString());
    }

    @Test
    void bookSession_mentorUnavailable() {
        // Given
        availableMentor.updateAvailability(false);

        BookSessionRequest request = new BookSessionRequest(
                mentorId,
                studentId,
                OffsetDateTime.now().plusDays(3),
                60,
                "OFFLINE",
                null,
                "Offline session at coaching center"
        );

        when(mentorProfileRepository.findById(mentorId)).thenReturn(Optional.of(availableMentor));

        // When / Then
        assertThatThrownBy(() -> mentorSessionService.bookSession(request))
                .isInstanceOf(SessionAlreadyBookedException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void completeSession_success() {
        // Given
        UUID sessionId = UUID.randomUUID();
        MentorSession session = MentorSession.create(
                availableMentor,
                studentId,
                OffsetDateTime.now().minusHours(2),
                60,
                SessionMode.ONLINE,
                "https://meet.edutech.com/done",
                "Completed session"
        );

        when(mentorSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(mentorSessionRepository.save(any(MentorSession.class))).thenReturn(session);
        when(mentorProfileRepository.save(any(MentorProfile.class))).thenReturn(availableMentor);

        // When
        MentorSessionResponse response = mentorSessionService.completeSession(sessionId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(SessionStatus.COMPLETED.name());
        verify(mentorSessionRepository, times(1)).save(any(MentorSession.class));
        verify(mentorProfileRepository, times(1)).save(any(MentorProfile.class));
        verify(mentorEventPublisher, times(1)).publishSessionCompleted(any());
    }

    @Test
    void getSessionsByStudent_success() {
        // Given
        MentorSession session1 = MentorSession.create(
                availableMentor,
                studentId,
                OffsetDateTime.now().plusDays(1),
                60,
                SessionMode.ONLINE,
                "https://meet.edutech.com/s1",
                null
        );
        MentorSession session2 = MentorSession.create(
                availableMentor,
                studentId,
                OffsetDateTime.now().plusDays(5),
                90,
                SessionMode.OFFLINE,
                null,
                "Mock test preparation"
        );

        when(mentorSessionRepository.findByStudentId(studentId)).thenReturn(List.of(session1, session2));

        // When
        List<MentorSessionResponse> responses = mentorSessionService.getSessionsByStudent(studentId);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).studentId()).isEqualTo(studentId);
        assertThat(responses.get(1).studentId()).isEqualTo(studentId);
    }
}
