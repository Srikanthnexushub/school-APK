// src/main/java/com/edutech/center/application/dto/CreateBannerRequest.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.BannerAudience;
import com.edutech.center.domain.model.BannerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request payload for creating a new platform banner.
 * SUPER_ADMIN only.
 *
 * <p>{@code bannerType} is optional — defaults to {@link BannerType#HERO} when null.
 */
public record CreateBannerRequest(

    @NotBlank
    String title,

    String subtitle,
    String imageUrl,
    String videoUrl,
    String linkUrl,
    String linkLabel,

    @NotNull
    BannerAudience audience,

    String bgColor,
    int displayOrder,
    Instant startDate,
    Instant endDate,

    BannerType bannerType

) {}
