// src/main/java/com/edutech/center/application/dto/UpdateStaffRequest.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.StaffRoleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * PATCH payload for updating a staff member's profile.
 * All fields are nullable — only non-null values are applied (partial update semantics).
 */
public record UpdateStaffRequest(

    @Size(max = 100)
    String firstName,

    @Size(max = 100)
    String lastName,

    @Size(max = 20)
    String phoneNumber,

    StaffRoleType roleType,

    @Size(max = 200)
    String designation,

    @Size(max = 500)
    String subjects,

    @Size(max = 100)
    String district,

    @Size(max = 500)
    String qualification,

    @Min(0) @Max(60)
    Integer yearsOfExperience,

    String bio

) {}
