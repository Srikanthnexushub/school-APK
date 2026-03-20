// src/main/java/com/edutech/center/domain/port/out/JobPostingRepository.java
package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.JobPosting;
import com.edutech.center.domain.model.JobPostingStatus;
import com.edutech.center.domain.model.JobType;
import com.edutech.center.domain.model.StaffRoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (output) — persistence contract for {@link JobPosting} aggregate.
 * Implemented by {@code JobPostingPersistenceAdapter} in the infrastructure layer.
 */
public interface JobPostingRepository {

    /**
     * Finds a non-deleted job posting by its ID, scoped to the given center.
     * Returns empty if the posting does not exist, belongs to a different center,
     * or has been soft-deleted.
     */
    Optional<JobPosting> findByIdAndCenterId(UUID id, UUID centerId);

    /**
     * Finds any job posting by its ID regardless of soft-deletion state.
     * Used when re-activation or admin recovery is needed.
     */
    Optional<JobPosting> findById(UUID id);

    /**
     * Returns all non-deleted job postings for a given center, across all statuses.
     * Used by the admin management view.
     */
    List<JobPosting> findByCenterId(UUID centerId);

    /**
     * Returns a paginated list of OPEN, non-deleted job postings for the public job board.
     * Supports optional filtering by role type, job type, and city (matched against the
     * posting's center city via a JOIN).
     *
     * @param roleType filter by role type; {@code null} means no filter
     * @param jobType  filter by job type; {@code null} means no filter
     * @param city     partial city name match (case-insensitive); {@code null} means no filter
     * @param pageable pagination and sort parameters
     */
    Page<JobPosting> findAllOpen(StaffRoleType roleType, JobType jobType,
                                 String city, Pageable pageable);

    /** Persists a new or updated {@link JobPosting} and returns the managed instance. */
    JobPosting save(JobPosting job);
}
