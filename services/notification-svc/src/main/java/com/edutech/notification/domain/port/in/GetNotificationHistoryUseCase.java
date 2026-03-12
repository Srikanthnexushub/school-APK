package com.edutech.notification.domain.port.in;

import com.edutech.notification.application.dto.NotificationHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GetNotificationHistoryUseCase {
    Page<NotificationHistoryResponse> getHistory(UUID recipientId, Pageable pageable);
    Page<NotificationHistoryResponse> getInAppNotifications(UUID recipientId, Pageable pageable);
    long countUnreadInApp(UUID recipientId);
}
