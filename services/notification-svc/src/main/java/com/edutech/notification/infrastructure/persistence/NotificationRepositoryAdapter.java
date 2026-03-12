package com.edutech.notification.infrastructure.persistence;

import com.edutech.notification.domain.model.Notification;
import com.edutech.notification.domain.model.NotificationChannel;
import com.edutech.notification.domain.port.out.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final SpringDataNotificationRepository springData;

    public NotificationRepositoryAdapter(SpringDataNotificationRepository springData) {
        this.springData = springData;
    }

    @Override
    public Notification save(Notification notification) {
        return springData.save(notification);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return springData.findById(id);
    }

    @Override
    public List<Notification> findPendingByChannel(NotificationChannel channel) {
        return springData.findByStatusAndChannel(
                com.edutech.notification.domain.model.NotificationStatus.PENDING, channel);
    }

    @Override
    public Page<Notification> findByRecipientId(UUID recipientId, Pageable pageable) {
        return springData.findByRecipientId(recipientId, pageable);
    }

    @Override
    public Page<Notification> findInAppByRecipientId(UUID recipientId, Pageable pageable) {
        return springData.findByRecipientIdAndChannel(recipientId, NotificationChannel.IN_APP, pageable);
    }

    @Override
    public long countUnreadInApp(UUID recipientId) {
        return springData.countUnreadByRecipientAndChannel(recipientId, NotificationChannel.IN_APP);
    }

    @Override
    public void markRead(UUID notificationId, UUID recipientId) {
        springData.markRead(notificationId, recipientId, Instant.now());
    }

    @Override
    public void markAllReadByRecipient(UUID recipientId) {
        springData.markAllRead(recipientId, NotificationChannel.IN_APP, Instant.now());
    }
}
