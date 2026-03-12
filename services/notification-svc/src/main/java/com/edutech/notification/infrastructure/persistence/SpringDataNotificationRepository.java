package com.edutech.notification.infrastructure.persistence;

import com.edutech.notification.domain.model.Notification;
import com.edutech.notification.domain.model.NotificationChannel;
import com.edutech.notification.domain.model.NotificationStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SpringDataNotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.channel = :channel ORDER BY n.createdAt ASC")
    List<Notification> findByStatusAndChannel(@Param("status") NotificationStatus status,
                                              @Param("channel") NotificationChannel channel);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientId(@Param("recipientId") UUID recipientId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.channel = :channel ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdAndChannel(@Param("recipientId") UUID recipientId,
                                                   @Param("channel") NotificationChannel channel,
                                                   Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId AND n.channel = :channel AND n.readAt IS NULL")
    long countUnreadByRecipientAndChannel(@Param("recipientId") UUID recipientId,
                                          @Param("channel") NotificationChannel channel);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.id = :id AND n.recipientId = :recipientId AND n.readAt IS NULL")
    void markRead(@Param("id") UUID id, @Param("recipientId") UUID recipientId, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.recipientId = :recipientId AND n.channel = :channel AND n.readAt IS NULL")
    void markAllRead(@Param("recipientId") UUID recipientId,
                     @Param("channel") NotificationChannel channel,
                     @Param("now") Instant now);
}
