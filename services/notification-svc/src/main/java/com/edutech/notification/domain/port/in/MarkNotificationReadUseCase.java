package com.edutech.notification.domain.port.in;

import java.util.UUID;

public interface MarkNotificationReadUseCase {
    void markRead(UUID notificationId, UUID userId);
    void markAllRead(UUID userId);
}
