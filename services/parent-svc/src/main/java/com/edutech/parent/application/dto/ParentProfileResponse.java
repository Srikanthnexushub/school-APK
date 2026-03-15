// src/main/java/com/edutech/parent/application/dto/ParentProfileResponse.java
package com.edutech.parent.application.dto;

import com.edutech.parent.domain.model.ParentStatus;

import java.time.Instant;
import java.util.UUID;

public record ParentProfileResponse(
        UUID id, UUID userId, String name, String phone, String email,
        String address, String city, String district, String state, String country, String pincode,
        String relationshipType, String occupation, String gender,
        boolean verified, ParentStatus status, Instant createdAt
) {}
