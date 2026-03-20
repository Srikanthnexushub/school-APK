// src/main/java/com/edutech/center/application/dto/UpdateBannerRequest.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.BannerAudience;
import com.edutech.center.domain.model.BannerType;

import java.time.Instant;

/**
 * Request payload for updating an existing platform banner (PATCH semantics).
 * All fields are nullable — only non-null fields are applied.
 * SUPER_ADMIN only.
 */
public record UpdateBannerRequest(

    String title,
    String subtitle,
    String imageUrl,
    String linkUrl,
    String linkLabel,
    BannerAudience audience,
    String bgColor,
    Integer displayOrder,
    Instant startDate,
    Instant endDate,
    BannerType bannerType

) {}
