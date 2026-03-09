package com.edutech.notification.infrastructure.persistence;

import com.edutech.notification.domain.model.Notification;
import com.edutech.notification.domain.model.NotificationChannel;
import com.edutech.notification.domain.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SpringDataNotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.channel = :channel ORDER BY n.createdAt ASC")
    List<Notification> findByStatusAndChannel(@Param("status") NotificationStatus status,
                                              @Param("channel") NotificationChannel channel);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientId(@Param("recipientId") UUID recipientId, Pageable pageable);
}
