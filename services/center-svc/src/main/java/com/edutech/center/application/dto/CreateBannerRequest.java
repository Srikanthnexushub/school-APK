// src/main/java/com/edutech/center/application/dto/CreateBannerRequest.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.BannerAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request payload for creating a new platform banner.
 * SUPER_ADMIN only.
 */
public record CreateBannerRequest(

    @NotBlank
    String title,

    String subtitle,
    String imageUrl,
    String linkUrl,
    String linkLabel,

    @NotNull
    BannerAudience audience,

    String bgColor,
    int displayOrder,
    Instant startDate,
    Instant endDate

) {}
