// src/main/java/com/edutech/center/application/dto/ContentItemResponse.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.ContentStatus;
import com.edutech.center.domain.model.ContentType;

import java.time.Instant;
import java.util.UUID;

public record ContentItemResponse(
    UUID id,
    UUID centerId,
    UUID batchId,
    String title,
    String description,
    ContentType type,
    String fileUrl,
    Long fileSizeBytes,
    UUID uploadedByUserId,
    ContentStatus status,
    Instant createdAt
) {}
