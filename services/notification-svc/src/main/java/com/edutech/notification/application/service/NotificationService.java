package com.edutech.notification.application.service;

import com.edutech.notification.application.dto.NotificationCommand;
import com.edutech.notification.application.dto.NotificationHistoryResponse;
import com.edutech.notification.domain.model.Notification;
import com.edutech.notification.domain.model.NotificationChannel;
import com.edutech.notification.domain.port.in.GetNotificationHistoryUseCase;
import com.edutech.notification.domain.port.in.MarkNotificationReadUseCase;
import com.edutech.notification.domain.port.in.SendNotificationUseCase;
import com.edutech.notification.domain.port.out.NotificationRepository;
import com.edutech.notification.domain.port.out.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationService implements SendNotificationUseCase,
        GetNotificationHistoryUseCase, MarkNotificationReadUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationService(NotificationRepository notificationRepository,
                                List<NotificationSender> senderList) {
        this.notificationRepository = notificationRepository;
        this.senders = senderList.stream()
                .collect(Collectors.toMap(NotificationSender::channel, Function.identity()));
    }

    @Override
    @Transactional
    public void send(NotificationCommand command) {
        NotificationChannel channel;
        try {
            channel = NotificationChannel.valueOf(command.channel());
        } catch (IllegalArgumentException ex) {
            log.error("Unknown notification channel '{}' — skipping", command.channel());
            return;
        }

        // Extract enrichment fields from metadata (sent by the publishing service)
        Map<String, String> meta = command.metadata() != null ? command.metadata() : Map.of();
        String notificationType = meta.get("notificationType");
        String actionUrl = meta.get("actionUrl");

        Notification notification = Notification.create(
                command.recipientId(),
                command.recipientEmail(),
                channel,
                command.subject(),
                command.body(),
                notificationType,
                actionUrl
        );
        notificationRepository.save(notification);

        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            log.error("No sender registered for channel {} — marking FAILED", channel);
            notification.markFailed("No sender registered for channel: " + channel);
            notificationRepository.save(notification);
            return;
        }

        try {
            sender.send(notification);
            notification.markSent();
            log.info("Notification sent: id={} channel={} type={} recipientId={}",
                    notification.getId(), channel, notificationType, notification.getRecipientId());
        } catch (Exception ex) {
            notification.markFailed(ex.getMessage());
            log.error("Notification delivery failed: id={} channel={} error={}",
                    notification.getId(), channel, ex.getMessage(), ex);
        }

        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationHistoryResponse> getHistory(UUID recipientId, Pageable pageable) {
        return notificationRepository.findByRecipientId(recipientId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationHistoryResponse> getInAppNotifications(UUID recipientId, Pageable pageable) {
        return notificationRepository.findInAppByRecipientId(recipientId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadInApp(UUID recipientId) {
        return notificationRepository.countUnreadInApp(recipientId);
    }

    @Override
    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        notificationRepository.markRead(notificationId, userId);
    }

    @Override
    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadByRecipient(userId);
    }

    private NotificationHistoryResponse toResponse(Notification n) {
        return new NotificationHistoryResponse(
                n.getId(),
                n.getChannel().name(),
                n.getSubject(),
                n.getBody(),
                n.getStatus().name(),
                n.getRetryCount(),
                n.getCreatedAt(),
                n.getSentAt(),
                n.getNotificationType(),
                n.getActionUrl(),
                n.getReadAt()
        );
    }
}
