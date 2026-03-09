package com.edutech.notification.domain.port.in;

import com.edutech.notification.application.dto.NotificationCommand;

public interface SendNotificationUseCase {
    void send(NotificationCommand command);
}
