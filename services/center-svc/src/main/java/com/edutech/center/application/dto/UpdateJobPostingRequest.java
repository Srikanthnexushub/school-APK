// src/main/java/com/edutech/center/application/dto/UpdateJobPostingRequest.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.JobType;
import com.edutech.center.domain.model.StaffRoleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for partially updating a job posting (PATCH semantics).
 * All fields are optional — only non-null fields are applied by the service.
 */
public record UpdateJobPostingRequest(

    @Size(max = 200)
    String title,

    @Size(max = 2000)
    String description,

    StaffRoleType roleType,

    String subjects,

    String qualifications,

    @Min(0) @Max(60)
    Integer experienceMinYears,

    JobType jobType,

    @Min(0)
    Integer salaryMin,

    @Min(0)
    Integer salaryMax,

    LocalDate deadline

) {}
