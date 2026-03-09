package com.edutech.notification.domain.port.out;

import com.edutech.notification.domain.model.Notification;
import com.edutech.notification.domain.model.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(UUID id);
    List<Notification> findPendingByChannel(NotificationChannel channel);
    Page<Notification> findByRecipientId(UUID recipientId, Pageable pageable);
}
