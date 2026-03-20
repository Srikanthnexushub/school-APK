// src/main/java/com/edutech/center/application/dto/BannerResponse.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.BannerAudience;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only view of a platform banner returned to API consumers.
 */
public record BannerResponse(

    UUID id,
    String title,
    String subtitle,
    String imageUrl,
    String linkUrl,
    String linkLabel,
    BannerAudience audience,
    String bgColor,
    int displayOrder,
    boolean isActive,
    Instant startDate,
    Instant endDate,
    Instant createdAt

) {}
