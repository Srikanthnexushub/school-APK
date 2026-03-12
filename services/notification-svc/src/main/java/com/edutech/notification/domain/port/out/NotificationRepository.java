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

    /** Returns paginated IN_APP notifications for a recipient, newest first. */
    Page<Notification> findInAppByRecipientId(UUID recipientId, Pageable pageable);

    /** Counts unread IN_APP notifications (readAt IS NULL). */
    long countUnreadInApp(UUID recipientId);

    /** Marks a single notification as read; no-op if already read or not owned by recipient. */
    void markRead(UUID notificationId, UUID recipientId);

    /** Marks all IN_APP notifications as read for the given recipient. */
    void markAllReadByRecipient(UUID recipientId);
}
