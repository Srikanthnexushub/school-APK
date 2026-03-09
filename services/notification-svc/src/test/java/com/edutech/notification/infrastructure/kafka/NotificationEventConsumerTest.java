package com.edutech.notification.infrastructure.kafka;

import com.edutech.events.notification.NotificationSendEvent;
import com.edutech.notification.application.dto.NotificationCommand;
import com.edutech.notification.domain.port.in.SendNotificationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private SendNotificationUseCase sendNotificationUseCase;

    private NotificationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationEventConsumer(sendNotificationUseCase);
    }

    @Test
    void onEmailEvent_delegatesToUseCase() {
        // Given
        UUID recipientId = UUID.randomUUID();
        NotificationSendEvent event = NotificationSendEvent.email(
                recipientId, "test@example.com", "Your OTP", "123456", Map.of("purpose", "LOGIN"));

        // When
        consumer.onNotificationSendEvent(event);

        // Then
        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(sendNotificationUseCase).send(captor.capture());

        NotificationCommand command = captor.getValue();
        assertThat(command.channel()).isEqualTo("EMAIL");
        assertThat(command.recipientId()).isEqualTo(recipientId);
        assertThat(command.recipientEmail()).isEqualTo("test@example.com");
        assertThat(command.subject()).isEqualTo("Your OTP");
        assertThat(command.body()).isEqualTo("123456");
    }

    @Test
    void onPushEvent_delegatesToUseCase() {
        // Given
        UUID recipientId = UUID.randomUUID();
        NotificationSendEvent event = NotificationSendEvent.push(
                recipientId, "New Result!", "Your exam results are available.", Map.of());

        // When
        consumer.onNotificationSendEvent(event);

        // Then
        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(sendNotificationUseCase).send(captor.capture());
        assertThat(captor.getValue().channel()).isEqualTo("PUSH");
    }

    @Test
    void onInAppEvent_delegatesToUseCase() {
        // Given
        NotificationSendEvent event = NotificationSendEvent.inApp(
                UUID.randomUUID(), "Reminder", "Session starts in 30 minutes.", Map.of());

        // When
        consumer.onNotificationSendEvent(event);

        // Then
        verify(sendNotificationUseCase).send(any());
    }

    @Test
    void onEvent_useCaseThrows_doesNotPropagateException() {
        // Given — consumer should be fault-tolerant; exceptions are logged, not rethrown
        NotificationSendEvent event = NotificationSendEvent.email(
                UUID.randomUUID(), "x@y.com", "Subject", "Body", Map.of());
        doThrow(new RuntimeException("DB down")).when(sendNotificationUseCase).send(any());

        // When / Then — must not throw (Kafka consumer cannot crash on single message failure)
        assertThatCode(() -> consumer.onNotificationSendEvent(event)).doesNotThrowAnyException();
    }

    @Test
    void onEvent_nullEmail_stillDelegates() {
        // Given — PUSH events have no email
        NotificationSendEvent event = NotificationSendEvent.push(
                UUID.randomUUID(), "Alert", "You've got mail.", null);

        // When
        consumer.onNotificationSendEvent(event);

        // Then
        verify(sendNotificationUseCase).send(any(NotificationCommand.class));
    }
}
