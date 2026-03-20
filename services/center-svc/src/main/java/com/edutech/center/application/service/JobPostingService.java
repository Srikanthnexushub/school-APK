// src/main/java/com/edutech/center/application/service/JobPostingService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CreateJobPostingRequest;
import com.edutech.center.application.dto.JobPostingResponse;
import com.edutech.center.application.dto.StatusUpdateRequest;
import com.edutech.center.application.dto.UpdateJobPostingRequest;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterNotFoundException;
import com.edutech.center.application.exception.JobPostingNotFoundException;
import com.edutech.center.domain.model.CoachingCenter;
import com.edutech.center.domain.model.JobPosting;
import com.edutech.center.domain.model.JobPostingStatus;
import com.edutech.center.domain.model.JobType;
import com.edutech.center.domain.model.StaffRoleType;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.domain.port.out.JobPostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for job posting management.
 *
 * <p>Covers the full lifecycle: creation, detail updates, status transitions,
 * soft-deletion, and paginated public job board queries.
 *
 * <p>All write operations require CENTER_ADMIN or SUPER_ADMIN access to the
 * given center. The public job board ({@link #listJobBoard}) is accessible
 * to any authenticated user.
 */
@Service
public class JobPostingService {

    private static final Logger log = LoggerFactory.getLogger(JobPostingService.class);

    private final JobPostingRepository jobPostingRepository;
    private final CenterRepository centerRepository;

    public JobPostingService(JobPostingRepository jobPostingRepository,
                             CenterRepository centerRepository) {
        this.jobPostingRepository = jobPostingRepository;
        this.centerRepository     = centerRepository;
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    /**
     * Creates a new job posting for the specified center.
     * When {@code req.status()} is {@code null}, the posting defaults to OPEN.
     */
    @Transactional
    public JobPostingResponse createJob(UUID centerId, CreateJobPostingRequest req,
                                        AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        CoachingCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new CenterNotFoundException(centerId));

        JobPosting job = JobPosting.create(
                centerId,
                req.title(), req.description(), req.roleType(),
                req.subjects(), req.qualifications(), req.experienceMinYears(),
                req.jobType(), req.salaryMin(), req.salaryMax(),
                req.deadline(), req.status());

        JobPosting saved = jobPostingRepository.save(job);
        log.info("Job posting created: jobId={} centerId={} roleType={} status={}",
                saved.getId(), centerId, req.roleType(), saved.getStatus());
        return toResponse(saved, center.getName(), center.getCity());
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    /**
     * Lists all non-deleted job postings for the center (all statuses).
     * Intended for the CENTER_ADMIN management view.
     */
    @Transactional(readOnly = true)
    public List<JobPostingResponse> listOwnJobs(UUID centerId, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        CoachingCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new CenterNotFoundException(centerId));

        return jobPostingRepository.findByCenterId(centerId).stream()
                .map(j -> toResponse(j, center.getName(), center.getCity()))
                .toList();
    }

    /**
     * Returns a single job posting scoped to the given center.
     */
    @Transactional(readOnly = true)
    public JobPostingResponse getJob(UUID centerId, UUID jobId, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        CoachingCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new CenterNotFoundException(centerId));

        JobPosting job = jobPostingRepository.findByIdAndCenterId(jobId, centerId)
                .orElseThrow(() -> new JobPostingNotFoundException(jobId));
        return toResponse(job, center.getName(), center.getCity());
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    /**
     * Partially updates the details of an existing job posting (PATCH semantics).
     * Only non-null fields in the request are applied.
     */
    @Transactional
    public JobPostingResponse updateJob(UUID centerId, UUID jobId,
                                        UpdateJobPostingRequest req, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        CoachingCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new CenterNotFoundException(centerId));

        JobPosting job = jobPostingRepository.findByIdAndCenterId(jobId, centerId)
                .orElseThrow(() -> new JobPostingNotFoundException(jobId));

        job.updateDetails(
                req.title(), req.description(), req.roleType(),
                req.subjects(), req.qualifications(), req.experienceMinYears(),
                req.jobType(), req.salaryMin(), req.salaryMax(), req.deadline());

        JobPosting saved = jobPostingRepository.save(job);
        log.info("Job posting updated: jobId={} centerId={}", jobId, centerId);
        return toResponse(saved, center.getName(), center.getCity());
    }

    /**
     * Transitions the status of a job posting.
     * Maps the requested {@link JobPostingStatus} to the appropriate domain method:
     * <ul>
     *   <li>OPEN   → {@code job.publish()}</li>
     *   <li>CLOSED → {@code job.close()}</li>
     *   <li>FILLED → {@code job.markFilled()}</li>
     *   <li>DRAFT  → {@code job.toDraft()}</li>
     * </ul>
     */
    @Transactional
    public JobPostingResponse updateStatus(UUID centerId, UUID jobId,
                                           StatusUpdateRequest req, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        CoachingCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new CenterNotFoundException(centerId));

        JobPosting job = jobPostingRepository.findByIdAndCenterId(jobId, centerId)
                .orElseThrow(() -> new JobPostingNotFoundException(jobId));

        switch (req.status()) {
            case OPEN   -> job.publish();
            case CLOSED -> job.close();
            case FILLED -> job.markFilled();
            case DRAFT  -> job.toDraft();
        }

        JobPosting saved = jobPostingRepository.save(job);
        log.info("Job posting status changed: jobId={} centerId={} newStatus={}",
                jobId, centerId, req.status());
        return toResponse(saved, center.getName(), center.getCity());
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    /**
     * Soft-deletes a job posting. The record is retained in the database
     * with {@code deletedAt} stamped and is excluded from all future queries.
     */
    @Transactional
    public void deleteJob(UUID centerId, UUID jobId, AuthPrincipal principal) {
        assertAdminAccess(centerId, principal);
        JobPosting job = jobPostingRepository.findByIdAndCenterId(jobId, centerId)
                .orElseThrow(() -> new JobPostingNotFoundException(jobId));

        job.softDelete();
        jobPostingRepository.save(job);
        log.info("Job posting soft-deleted: jobId={} centerId={}", jobId, centerId);
    }

    // ─── Public job board ─────────────────────────────────────────────────────

    /**
     * Returns a paginated list of OPEN job postings for the public job board.
     * No admin access check — any authenticated user may browse.
     * Center name and city are enriched per page item via a repository lookup.
     * If the center is not found (data inconsistency), name and city fall back to
     * empty strings so the response is never broken.
     *
     * @param roleType optional role type filter
     * @param jobType  optional job type filter
     * @param city     optional partial city name filter (case-insensitive)
     * @param pageable pagination parameters
     */
    @Transactional(readOnly = true)
    public Page<JobPostingResponse> listJobBoard(StaffRoleType roleType, JobType jobType,
                                                 String city, Pageable pageable) {
        Page<JobPosting> page = jobPostingRepository.findAllOpen(roleType, jobType, city, pageable);
        return page.map(job -> {
            String centerName = "";
            String centerCity = "";
            try {
                CoachingCenter center = centerRepository.findById(job.getCenterId())
                        .orElse(null);
                if (center != null) {
                    centerName = center.getName();
                    centerCity = center.getCity();
                }
            } catch (Exception ex) {
                log.warn("Could not enrich center details for jobId={}: {}", job.getId(), ex.getMessage());
            }
            return toResponse(job, centerName, centerCity);
        });
    }

    // ─── Access control ───────────────────────────────────────────────────────

    private void assertAdminAccess(UUID centerId, AuthPrincipal principal) {
        if (principal.isSuperAdmin()) return;
        if (principal.belongsToCenter(centerId)) return;
        boolean isOwningAdmin = centerRepository.findById(centerId)
                .map(c -> principal.belongsToCenter(centerId, c.getAdminUserId()))
                .orElse(false);
        if (!isOwningAdmin) throw new CenterAccessDeniedException();
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private JobPostingResponse toResponse(JobPosting j, String centerName, String centerCity) {
        return new JobPostingResponse(
                j.getId(), j.getCenterId(), centerName, centerCity,
                j.getTitle(), j.getDescription(), j.getRoleType(),
                j.getSubjects(), j.getQualifications(), j.getExperienceMinYears(),
                j.getJobType(), j.getSalaryMin(), j.getSalaryMax(),
                j.getDeadline(), j.getStatus(), j.getPostedAt(), j.getUpdatedAt());
    }
}
