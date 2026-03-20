// src/main/java/com/edutech/center/application/dto/CreateJobPostingRequest.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.JobPostingStatus;
import com.edutech.center.domain.model.JobType;
import com.edutech.center.domain.model.StaffRoleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for creating a new job posting.
 *
 * <p>{@code status} is optional — when {@code null} the service defaults to
 * {@link JobPostingStatus#OPEN}. Pass {@link JobPostingStatus#DRAFT} to save
 * the posting without publishing it immediately.
 */
public record CreateJobPostingRequest(

    @NotBlank @Size(max = 200)
    String title,

    @Size(max = 2000)
    String description,

    @NotNull
    StaffRoleType roleType,

    /** Comma-separated subject names (e.g. "Mathematics,Physics"). Nullable for non-teaching roles. */
    String subjects,

    /** Comma-separated qualification requirements. Nullable. */
    String qualifications,

    @Min(0) @Max(60)
    Integer experienceMinYears,

    @NotNull
    JobType jobType,

    @Min(0)
    Integer salaryMin,

    @Min(0)
    Integer salaryMax,

    LocalDate deadline,

    /** Optional initial status. Defaults to OPEN when null. */
    JobPostingStatus status

) {}
