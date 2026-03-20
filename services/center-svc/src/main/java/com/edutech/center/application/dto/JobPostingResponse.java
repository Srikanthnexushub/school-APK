// src/main/java/com/edutech/center/application/dto/JobPostingResponse.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.JobPostingStatus;
import com.edutech.center.domain.model.JobType;
import com.edutech.center.domain.model.StaffRoleType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only view of a job posting returned to API consumers.
 * Includes denormalized center name and city for convenience (avoids a
 * separate center lookup on the client).
 */
public record JobPostingResponse(

    UUID id,
    UUID centerId,
    String centerName,
    String centerCity,

    String title,
    String description,
    StaffRoleType roleType,
    String subjects,
    String qualifications,
    Integer experienceMinYears,
    JobType jobType,
    Integer salaryMin,
    Integer salaryMax,
    LocalDate deadline,

    JobPostingStatus status,
    Instant postedAt,
    Instant updatedAt

) {}
