// src/main/java/com/edutech/center/application/dto/CenterResponse.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.CenterStatus;

import java.time.Instant;
import java.util.UUID;

public record CenterResponse(
    UUID id,
    String name,
    String code,
    String address,
    String city,
    String state,
    String pincode,
    String phone,
    String email,
    String website,
    String logoUrl,
    CenterStatus status,
    UUID ownerId,
    Instant createdAt,
    Instant updatedAt,
    String branch,
    String board
) {}
