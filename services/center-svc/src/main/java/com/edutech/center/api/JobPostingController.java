// src/main/java/com/edutech/center/api/JobPostingController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CreateJobPostingRequest;
import com.edutech.center.application.dto.JobPostingResponse;
import com.edutech.center.application.dto.StatusUpdateRequest;
import com.edutech.center.application.dto.UpdateJobPostingRequest;
import com.edutech.center.application.service.JobPostingService;
import com.edutech.center.domain.model.JobType;
import com.edutech.center.domain.model.StaffRoleType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for job posting management and the public job board.
 *
 * <p>Center-scoped endpoints ({@code /api/v1/centers/{centerId}/jobs}) require
 * CENTER_ADMIN or SUPER_ADMIN access.
 *
 * <p>The public job board ({@code GET /api/v1/jobs}) is available to any authenticated user.
 */
@RestController
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Jobs", description = "Job posting management for coaching centers and public job board")
public class JobPostingController {

    private final JobPostingService jobPostingService;

    public JobPostingController(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }

    // ─── Center-scoped endpoints ───────────────────────────────────────────────

    /**
     * Creates a new job posting for the center.
     * Pass {@code status: "DRAFT"} in the body to save without publishing;
     * omit {@code status} or pass {@code "OPEN"} to publish immediately.
     */
    @PostMapping("/api/v1/centers/{centerId}/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a job posting for a center (admin only)")
    public JobPostingResponse createJob(@PathVariable UUID centerId,
                                        @Valid @RequestBody CreateJobPostingRequest request,
                                        @AuthenticationPrincipal AuthPrincipal principal) {
        return jobPostingService.createJob(centerId, request, principal);
    }

    /**
     * Lists all non-deleted job postings for the center (all statuses).
     * Intended for the CENTER_ADMIN management dashboard.
     */
    @GetMapping("/api/v1/centers/{centerId}/jobs")
    @Operation(summary = "List all job postings for a center (admin only)")
    public List<JobPostingResponse> listOwnJobs(@PathVariable UUID centerId,
                                                @AuthenticationPrincipal AuthPrincipal principal) {
        return jobPostingService.listOwnJobs(centerId, principal);
    }

    /**
     * Returns a single job posting by ID, scoped to the center.
     */
    @GetMapping("/api/v1/centers/{centerId}/jobs/{jobId}")
    @Operation(summary = "Get a single job posting (admin only)")
    public JobPostingResponse getJob(@PathVariable UUID centerId,
                                     @PathVariable UUID jobId,
                                     @AuthenticationPrincipal AuthPrincipal principal) {
        return jobPostingService.getJob(centerId, jobId, principal);
    }

    /**
     * Partially updates job posting details (PATCH semantics — only non-null fields applied).
     */
    @PutMapping("/api/v1/centers/{centerId}/jobs/{jobId}")
    @Operation(summary = "Update job posting details (admin only)")
    public JobPostingResponse updateJob(@PathVariable UUID centerId,
                                        @PathVariable UUID jobId,
                                        @Valid @RequestBody UpdateJobPostingRequest request,
                                        @AuthenticationPrincipal AuthPrincipal principal) {
        return jobPostingService.updateJob(centerId, jobId, request, principal);
    }

    /**
     * Transitions the status of a job posting.
     *
     * <p>Valid transitions:
     * <ul>
     *   <li>DRAFT  → OPEN   (publish)</li>
     *   <li>OPEN   → CLOSED (close)</li>
     *   <li>OPEN   → FILLED (mark as filled)</li>
     *   <li>CLOSED → DRAFT  (revert to draft)</li>
     *   <li>FILLED → DRAFT  (revert to draft)</li>
     * </ul>
     */
    @PatchMapping("/api/v1/centers/{centerId}/jobs/{jobId}/status")
    @Operation(summary = "Transition the status of a job posting (admin only)")
    public JobPostingResponse updateStatus(@PathVariable UUID centerId,
                                           @PathVariable UUID jobId,
                                           @Valid @RequestBody StatusUpdateRequest request,
                                           @AuthenticationPrincipal AuthPrincipal principal) {
        return jobPostingService.updateStatus(centerId, jobId, request, principal);
    }

    /**
     * Soft-deletes a job posting. The record is retained for audit purposes
     * but will no longer appear in any listings.
     */
    @DeleteMapping("/api/v1/centers/{centerId}/jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a job posting (admin only)")
    public void deleteJob(@PathVariable UUID centerId,
                          @PathVariable UUID jobId,
                          @AuthenticationPrincipal AuthPrincipal principal) {
        jobPostingService.deleteJob(centerId, jobId, principal);
    }

    // ─── Public job board ─────────────────────────────────────────────────────

    /**
     * Public job board — returns paginated OPEN job postings across all centers.
     * Available to any authenticated user (no role restriction).
     *
     * <p>Query parameters:
     * <ul>
     *   <li>{@code roleType} — filter by staff role type (e.g. {@code TEACHER}, {@code HOD})</li>
     *   <li>{@code jobType}  — filter by employment type (e.g. {@code FULL_TIME}, {@code CONTRACT})</li>
     *   <li>{@code city}     — partial, case-insensitive city name filter</li>
     *   <li>{@code page}, {@code size}, {@code sort} — standard Spring pagination</li>
     * </ul>
     */
    @GetMapping("/api/v1/jobs")
    @Operation(summary = "Browse public job board — OPEN postings across all centers")
    public Page<JobPostingResponse> listJobBoard(
            @RequestParam(required = false) StaffRoleType roleType,
            @RequestParam(required = false) JobType jobType,
            @RequestParam(required = false) String city,
            @PageableDefault(size = 20, sort = "postedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return jobPostingService.listJobBoard(roleType, jobType, city, pageable);
    }
}
