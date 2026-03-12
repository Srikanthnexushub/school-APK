package com.edutech.notification.api;

import com.edutech.notification.application.dto.NotificationHistoryResponse;
import com.edutech.notification.domain.port.in.GetNotificationHistoryUseCase;
import com.edutech.notification.domain.port.in.MarkNotificationReadUseCase;
import com.edutech.notification.infrastructure.sse.SseEmitterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private GetNotificationHistoryUseCase getNotificationHistoryUseCase;

    @Mock
    private MarkNotificationReadUseCase markNotificationReadUseCase;

    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    private NotificationController controller;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new NotificationController(
                getNotificationHistoryUseCase, markNotificationReadUseCase, sseEmitterRegistry);
    }

    @Test
    void getMyNotifications_returnsPagedResults() {
        NotificationHistoryResponse item = new NotificationHistoryResponse(
                UUID.randomUUID(), "EMAIL", "Test Subject", "Test body",
                "SENT", 0, Instant.now(), Instant.now(), null, null, null);
        Pageable pageable = PageRequest.of(0, 20);
        Page<NotificationHistoryResponse> page = new PageImpl<>(List.of(item), pageable, 1);
        when(getNotificationHistoryUseCase.getHistory(eq(USER_ID), any())).thenReturn(page);

        ResponseEntity<Page<NotificationHistoryResponse>> response =
                controller.getMyNotifications(USER_ID.toString(), pageable);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalElements()).isEqualTo(1);
        assertThat(response.getBody().getContent().get(0).channel()).isEqualTo("EMAIL");
    }

    @Test
    void getMyNotifications_delegatesRecipientIdToUseCase() {
        Pageable pageable = PageRequest.of(0, 20);
        when(getNotificationHistoryUseCase.getHistory(eq(USER_ID), any())).thenReturn(Page.empty());

        controller.getMyNotifications(USER_ID.toString(), pageable);

        verify(getNotificationHistoryUseCase).getHistory(eq(USER_ID), any());
    }

    @Test
    void getMyNotifications_emptyResults_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(getNotificationHistoryUseCase.getHistory(any(), any())).thenReturn(Page.empty());

        ResponseEntity<Page<NotificationHistoryResponse>> response =
                controller.getMyNotifications(USER_ID.toString(), pageable);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isEmpty()).isTrue();
    }

    @Test
    void getMyNotifications_secondPage_passesPageableToUseCase() {
        Pageable page2 = PageRequest.of(1, 10);
        when(getNotificationHistoryUseCase.getHistory(any(), eq(page2))).thenReturn(Page.empty());

        controller.getMyNotifications(USER_ID.toString(), page2);

        verify(getNotificationHistoryUseCase).getHistory(eq(USER_ID), eq(page2));
    }
}
