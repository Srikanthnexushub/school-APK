// src/main/java/com/edutech/center/application/dto/CreateStaffRequest.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.StaffRoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new staff member via admin-initiated invitation.
 * The created record starts in INVITATION_SENT status; the staff member activates
 * it by accepting the emailed invitation and completing registration.
 */
public record CreateStaffRequest(

    @NotBlank @Size(max = 100)
    String firstName,

    @NotBlank @Size(max = 100)
    String lastName,

    @NotBlank @Email @Size(max = 255)
    String email,

    @Size(max = 20)
    String phoneNumber,

    @Size(max = 50)
    String employeeId,

    @NotNull
    StaffRoleType roleType,

    /** Free-text designation, e.g. "Senior Teacher", "HOD Physics". */
    @Size(max = 200)
    String designation,

    /** Comma-separated canonical subject names from SubjectCatalog. */
    @Size(max = 500)
    String subjects,

    @Size(max = 100)
    String district,

    /**
     * Highest qualification held, e.g. "B.Ed, M.Sc Mathematics".
     * Multiple qualifications may be listed comma-separated.
     */
    @Size(max = 500)
    String qualification,

    /** Total years of professional experience. 0 = fresher. */
    @Min(0) @Max(60)
    Integer yearsOfExperience,

    /**
     * Professional bio — may be AI-generated on the frontend before submission.
     * Stored as-is; no length cap enforced here (DB column is TEXT).
     */
    String bio

) {}
