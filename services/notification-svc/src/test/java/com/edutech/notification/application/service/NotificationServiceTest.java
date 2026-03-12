package com.edutech.notification.application.service;

import com.edutech.notification.application.dto.NotificationCommand;
import com.edutech.notification.application.dto.NotificationHistoryResponse;
import com.edutech.notification.domain.model.Notification;
import com.edutech.notification.domain.model.NotificationChannel;
import com.edutech.notification.domain.model.NotificationStatus;
import com.edutech.notification.domain.port.out.NotificationRepository;
import com.edutech.notification.domain.port.out.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSender emailSender;

    private NotificationService notificationService;

    private final UUID recipientId = UUID.randomUUID();
    private final String recipientEmail = "student@test.com";

    @BeforeEach
    void setUp() {
        when(emailSender.channel()).thenReturn(NotificationChannel.EMAIL);
        notificationService = new NotificationService(notificationRepository, List.of(emailSender));
    }

    @Test
    void send_email_success() {
        // Given
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        NotificationCommand command = new NotificationCommand(
                "EMAIL", recipientId, recipientEmail, null, "Welcome!", "Welcome to EduTech.", Map.of());

        // When
        notificationService.send(command);

        // Then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        verify(emailSender).send(any());

        Notification saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.getRecipientId()).isEqualTo(recipientId);
    }

    @Test
    void send_unknownChannel_skipsProcessing() {
        // Given
        NotificationCommand command = new NotificationCommand(
                "TELEGRAM", recipientId, recipientEmail, null, "Test", "Body", Map.of());

        // When
        notificationService.send(command);

        // Then — no persistence, no delivery
        verify(notificationRepository, never()).save(any());
        verify(emailSender, never()).send(any());
    }

    @Test
    void send_noSenderForChannel_marksFailedAndSaves() {
        // Given — channel is IN_APP but no IN_APP sender registered
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        NotificationCommand command = new NotificationCommand(
                "IN_APP", recipientId, null, null, "Alert", "You have a new message.", Map.of());

        // When
        notificationService.send(command);

        // Then — saved once as PENDING then once as FAILED
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void send_deliveryFails_marksNotificationFailed() {
        // Given
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP timeout")).when(emailSender).send(any());

        NotificationCommand command = new NotificationCommand(
                "EMAIL", recipientId, recipientEmail, null, "Test", "Body", Map.of());

        // When
        notificationService.send(command);

        // Then — status becomes FAILED, retry_count incremented
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        Notification failedNotification = captor.getValue();
        assertThat(failedNotification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(failedNotification.getRetryCount()).isEqualTo(1);
        assertThat(failedNotification.getErrorMessage()).contains("SMTP timeout");
    }

    @Test
    void getHistory_returnsPagedResults() {
        // Given
        Notification n = Notification.create(recipientId, recipientEmail,
                NotificationChannel.EMAIL, "Hello", "Body");
        Page<Notification> page = new PageImpl<>(List.of(n));
        Pageable pageable = PageRequest.of(0, 20);
        when(notificationRepository.findByRecipientId(recipientId, pageable)).thenReturn(page);

        // When
        Page<NotificationHistoryResponse> result = notificationService.getHistory(recipientId, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        NotificationHistoryResponse dto = result.getContent().get(0);
        assertThat(dto.channel()).isEqualTo("EMAIL");
        assertThat(dto.subject()).isEqualTo("Hello");
        assertThat(dto.status()).isEqualTo("PENDING");
    }

    @Test
    void getHistory_emptyRecipient_returnsEmptyPage() {
        // Given
        UUID otherId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        when(notificationRepository.findByRecipientId(otherId, pageable))
                .thenReturn(Page.empty(pageable));

        // When
        Page<NotificationHistoryResponse> result = notificationService.getHistory(otherId, pageable);

        // Then
        assertThat(result.isEmpty()).isTrue();
    }
}
