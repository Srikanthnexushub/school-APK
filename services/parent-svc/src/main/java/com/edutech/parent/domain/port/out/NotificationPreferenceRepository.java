// src/main/java/com/edutech/parent/domain/port/out/NotificationPreferenceRepository.java
package com.edutech.parent.domain.port.out;

import com.edutech.parent.domain.model.NotificationChannel;
import com.edutech.parent.domain.model.NotificationPreference;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository {
    NotificationPreference save(NotificationPreference pref);
    Optional<NotificationPreference> findById(UUID id);
    List<NotificationPreference> findByParentId(UUID parentId);
    Optional<NotificationPreference> findByParentIdAndChannelAndEventType(UUID parentId, NotificationChannel channel, String eventType);
}
