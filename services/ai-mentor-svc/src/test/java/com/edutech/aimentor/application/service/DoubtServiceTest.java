package com.edutech.aimentor.application.service;

import com.edutech.aimentor.application.dto.DoubtTicketResponse;
import com.edutech.aimentor.application.dto.SubmitDoubtRequest;
import com.edutech.aimentor.application.exception.DoubtNotFoundException;
import com.edutech.aimentor.domain.event.DoubtSubmittedEvent;
import com.edutech.aimentor.domain.model.DoubtStatus;
import com.edutech.aimentor.domain.model.DoubtTicket;
import com.edutech.aimentor.domain.model.SubjectArea;
import com.edutech.aimentor.domain.port.out.AiGatewayClient;
import com.edutech.aimentor.domain.port.out.AiMentorEventPublisher;
import com.edutech.aimentor.domain.port.out.DoubtTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DoubtService unit tests")
class DoubtServiceTest {

    @Mock
    private DoubtTicketRepository doubtTicketRepository;

    @Mock
    private AiGatewayClient aiGatewayClient;

    @Mock
    private AiMentorEventPublisher eventPublisher;

    private DoubtService doubtService;

    @BeforeEach
    void setUp() {
        RagContextBuilder ragContextBuilder = new RagContextBuilder();
        doubtService = new DoubtService(doubtTicketRepository, aiGatewayClient, eventPublisher, ragContextBuilder);
    }

    @Test
    @DisplayName("submitDoubt_success: creates ticket, saves it, and publishes DoubtSubmittedEvent")
    void submitDoubt_success() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        SubmitDoubtRequest request = new SubmitDoubtRequest(
                studentId, enrollmentId, SubjectArea.PHYSICS,
                "What is Newton's second law?"
        );

        when(aiGatewayClient.resolveDoubt(anyString(), eq(SubjectArea.PHYSICS)))
                .thenReturn("F = ma, where F is force, m is mass, and a is acceleration.");

        DoubtTicket savedTicket = DoubtTicket.create(studentId, enrollmentId,
                SubjectArea.PHYSICS, "What is Newton's second law?");
        savedTicket.resolve("F = ma, where F is force, m is mass, and a is acceleration.");
        when(doubtTicketRepository.save(any(DoubtTicket.class))).thenReturn(savedTicket);

        // When
        doubtService.submitDoubt(request);

        // Then
        verify(doubtTicketRepository).save(any(DoubtTicket.class));

        ArgumentCaptor<DoubtSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(DoubtSubmittedEvent.class);
        verify(eventPublisher).publishDoubtSubmitted(eventCaptor.capture());

        DoubtSubmittedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.studentId()).isEqualTo(studentId);
        assertThat(publishedEvent.enrollmentId()).isEqualTo(enrollmentId);
        assertThat(publishedEvent.subjectArea()).isEqualTo(SubjectArea.PHYSICS);
    }

    @Test
    @DisplayName("submitDoubt_aiResolution: when AI returns answer, ticket status is RESOLVED")
    void submitDoubt_aiResolution() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        SubmitDoubtRequest request = new SubmitDoubtRequest(
                studentId, enrollmentId, SubjectArea.CHEMISTRY,
                "Explain the Aufbau principle."
        );

        String aiAnswer = "The Aufbau principle states that electrons fill atomic orbitals from lowest to highest energy.";
        when(aiGatewayClient.resolveDoubt(anyString(), eq(SubjectArea.CHEMISTRY))).thenReturn(aiAnswer);

        ArgumentCaptor<DoubtTicket> ticketCaptor = ArgumentCaptor.forClass(DoubtTicket.class);
        when(doubtTicketRepository.save(ticketCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // When
        DoubtTicketResponse response = doubtService.submitDoubt(request);

        // Then
        DoubtTicket savedTicket = ticketCaptor.getValue();
        assertThat(savedTicket.getStatus()).isEqualTo(DoubtStatus.RESOLVED);
        assertThat(savedTicket.getAiAnswer()).isEqualTo(aiAnswer);
        assertThat(savedTicket.getResolvedAt()).isNotNull();

        assertThat(response.status()).isEqualTo(DoubtStatus.RESOLVED);
        assertThat(response.aiAnswer()).isEqualTo(aiAnswer);
    }

    @Test
    @DisplayName("submitDoubt_aiFailure: when AI gateway throws, ticket status remains PENDING")
    void submitDoubt_aiFailure() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        SubmitDoubtRequest request = new SubmitDoubtRequest(
                studentId, enrollmentId, SubjectArea.MATHEMATICS,
                "What is the integral of sin(x)?"
        );

        when(aiGatewayClient.resolveDoubt(anyString(), eq(SubjectArea.MATHEMATICS)))
                .thenThrow(new RuntimeException("AI gateway connection timeout"));

        ArgumentCaptor<DoubtTicket> ticketCaptor = ArgumentCaptor.forClass(DoubtTicket.class);
        when(doubtTicketRepository.save(ticketCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // When
        DoubtTicketResponse response = doubtService.submitDoubt(request);

        // Then — graceful degradation: ticket saved in PENDING state, no exception thrown
        DoubtTicket savedTicket = ticketCaptor.getValue();
        assertThat(savedTicket.getStatus()).isEqualTo(DoubtStatus.PENDING);
        assertThat(savedTicket.getAiAnswer()).isNull();

        assertThat(response.status()).isEqualTo(DoubtStatus.PENDING);

        // DoubtResolvedEvent must NOT be published (ticket is PENDING)
        verify(eventPublisher, never()).publishDoubtResolved(any());
    }

    @Test
    @DisplayName("getDoubt_found: returns DoubtTicketResponse when ticket exists for the student")
    void getDoubt_found() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        DoubtTicket ticket = DoubtTicket.create(studentId, enrollmentId,
                SubjectArea.BIOLOGY, "What is mitosis?");
        UUID ticketId = ticket.getId();

        when(doubtTicketRepository.findByIdAndStudentId(ticketId, studentId))
                .thenReturn(Optional.of(ticket));

        // When
        DoubtTicketResponse response = doubtService.getDoubt(ticketId, studentId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(ticketId);
        assertThat(response.studentId()).isEqualTo(studentId);
        assertThat(response.subjectArea()).isEqualTo(SubjectArea.BIOLOGY);
        assertThat(response.question()).isEqualTo("What is mitosis?");
        assertThat(response.status()).isEqualTo(DoubtStatus.PENDING);
    }

    @Test
    @DisplayName("getDoubt_notFound: throws DoubtNotFoundException when ticket does not exist")
    void getDoubt_notFound() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        when(doubtTicketRepository.findByIdAndStudentId(ticketId, studentId))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> doubtService.getDoubt(ticketId, studentId))
                .isInstanceOf(DoubtNotFoundException.class)
                .hasMessageContaining(ticketId.toString());
    }
}
