// src/main/java/com/edutech/parent/infrastructure/persistence/NotificationPreferencePersistenceAdapter.java
package com.edutech.parent.infrastructure.persistence;

import com.edutech.parent.domain.model.NotificationChannel;
import com.edutech.parent.domain.model.NotificationPreference;
import com.edutech.parent.domain.port.out.NotificationPreferenceRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class NotificationPreferencePersistenceAdapter implements NotificationPreferenceRepository {

    private final SpringDataNotificationPreferenceRepository repository;

    NotificationPreferencePersistenceAdapter(SpringDataNotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationPreference save(NotificationPreference pref) {
        return repository.save(pref);
    }

    @Override
    public Optional<NotificationPreference> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<NotificationPreference> findByParentId(UUID parentId) {
        return repository.findByParentId(parentId);
    }

    @Override
    public Optional<NotificationPreference> findByParentIdAndChannelAndEventType(UUID parentId,
                                                                                  NotificationChannel channel,
                                                                                  String eventType) {
        return repository.findByParentIdAndChannelAndEventType(parentId, channel, eventType);
    }
}
