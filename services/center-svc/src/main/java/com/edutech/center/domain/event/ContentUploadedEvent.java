// src/main/java/com/edutech/center/domain/event/ContentUploadedEvent.java
package com.edutech.center.domain.event;

import com.edutech.center.domain.model.ContentType;

import java.time.Instant;
import java.util.UUID;

public record ContentUploadedEvent(
    UUID eventId,
    UUID contentId,
    UUID centerId,
    UUID batchId,
    String title,
    ContentType type,
    UUID uploadedByUserId,
    Instant occurredAt
) {
    public ContentUploadedEvent(UUID contentId, UUID centerId, UUID batchId,
                                String title, ContentType type, UUID uploadedByUserId) {
        this(UUID.randomUUID(), contentId, centerId, batchId, title, type, uploadedByUserId, Instant.now());
    }
}
