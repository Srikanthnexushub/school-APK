// src/main/java/com/edutech/parent/infrastructure/persistence/SpringDataNotificationPreferenceRepository.java
package com.edutech.parent.infrastructure.persistence;

import com.edutech.parent.domain.model.NotificationChannel;
import com.edutech.parent.domain.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataNotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    @Query("SELECT p FROM NotificationPreference p WHERE p.parentId = :parentId ORDER BY p.channel, p.eventType")
    List<NotificationPreference> findByParentId(@Param("parentId") UUID parentId);

    @Query("SELECT p FROM NotificationPreference p WHERE p.parentId = :parentId AND p.channel = :channel AND p.eventType = :eventType")
    Optional<NotificationPreference> findByParentIdAndChannelAndEventType(
        @Param("parentId") UUID parentId,
        @Param("channel") NotificationChannel channel,
        @Param("eventType") String eventType);
}
