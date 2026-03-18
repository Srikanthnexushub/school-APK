// src/main/java/com/edutech/center/application/dto/TeacherResponse.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.StaffRoleType;
import com.edutech.center.domain.model.TeacherStatus;

import java.time.Instant;
import java.util.UUID;

public record TeacherResponse(
    UUID id,
    UUID centerId,
    UUID userId,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    String subjects,
    String district,
    String employeeId,
    TeacherStatus status,
    Instant joinedAt,
    // ── Staff profile fields (V13) ────────────────────────────────────────────
    StaffRoleType roleType,
    String qualification,
    Integer yearsOfExperience,
    String designation,
    String bio
) {}
