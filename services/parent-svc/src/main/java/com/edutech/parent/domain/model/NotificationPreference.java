// src/main/java/com/edutech/parent/domain/model/NotificationPreference.java
package com.edutech.parent.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences", schema = "parent_schema")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private boolean enabled;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private NotificationPreference() {}

    public static NotificationPreference create(UUID parentId, NotificationChannel channel,
                                                 String eventType, boolean enabled) {
        NotificationPreference pref = new NotificationPreference();
        pref.parentId = parentId;
        pref.channel = channel;
        pref.eventType = eventType;
        pref.enabled = enabled;
        pref.createdAt = Instant.now();
        pref.updatedAt = pref.createdAt;
        return pref;
    }

    public void toggle(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getParentId() {
        return parentId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getEventType() {
        return eventType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
