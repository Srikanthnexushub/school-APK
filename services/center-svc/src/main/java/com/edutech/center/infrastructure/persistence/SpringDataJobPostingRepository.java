// src/main/java/com/edutech/center/infrastructure/persistence/SpringDataJobPostingRepository.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.JobPosting;
import com.edutech.center.domain.model.JobType;
import com.edutech.center.domain.model.StaffRoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link JobPosting}.
 * Package-private — accessed only through {@link JobPostingPersistenceAdapter}.
 */
interface SpringDataJobPostingRepository extends JpaRepository<JobPosting, UUID> {

    @Query("SELECT j FROM JobPosting j WHERE j.id = :id AND j.centerId = :centerId AND j.deletedAt IS NULL")
    Optional<JobPosting> findByIdAndCenterIdActive(@Param("id") UUID id,
                                                   @Param("centerId") UUID centerId);

    @Query("SELECT j FROM JobPosting j WHERE j.centerId = :centerId AND j.deletedAt IS NULL ORDER BY j.postedAt DESC")
    List<JobPosting> findByCenterIdActive(@Param("centerId") UUID centerId);

    /**
     * Public job board query — OPEN, non-deleted postings with optional filters.
     * Joins with {@link com.edutech.center.domain.model.CoachingCenter} to support city filtering.
     * Uses CAST(:city AS String) to prevent Hibernate from binding the null value as bytea on PostgreSQL.
     */
    @Query("""
            SELECT j FROM JobPosting j
            JOIN CoachingCenter c ON j.centerId = c.id
            WHERE j.deletedAt IS NULL
              AND j.status = 'OPEN'
              AND (:roleType IS NULL OR j.roleType = :roleType)
              AND (:jobType  IS NULL OR j.jobType  = :jobType)
              AND (CAST(:city AS String) IS NULL OR LOWER(c.city) LIKE LOWER(CONCAT('%', CAST(:city AS String), '%')))
            ORDER BY j.postedAt DESC
            """)
    Page<JobPosting> findAllOpen(@Param("roleType") StaffRoleType roleType,
                                 @Param("jobType")  JobType jobType,
                                 @Param("city")     String city,
                                 Pageable pageable);
}
