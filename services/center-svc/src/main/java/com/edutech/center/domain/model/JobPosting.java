// src/main/java/com/edutech/center/domain/model/JobPosting.java
package com.edutech.center.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A job posting published by a coaching center.
 *
 * <p>State is mutated only through named domain methods — no public setters.
 * Soft-deletion via {@code deletedAt}; hard-deletes are never performed.
 */
@Entity
@Table(name = "job_postings", schema = "center_schema")
public class JobPosting {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "center_id", updatable = false, nullable = false)
    private UUID centerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 50)
    private StaffRoleType roleType;

    /** Comma-separated subject names; nullable (non-teaching roles may omit). */
    @Column(columnDefinition = "TEXT")
    private String subjects;

    /** Comma-separated qualification requirements; nullable. */
    @Column(columnDefinition = "TEXT")
    private String qualifications;

    @Column(name = "experience_min_years")
    private Integer experienceMinYears;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 30)
    private JobType jobType;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobPostingStatus status;

    @Column(name = "posted_at", updatable = false, nullable = false)
    private Instant postedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    /** Required by JPA. */
    protected JobPosting() {}

    private JobPosting(UUID id, UUID centerId, String title, String description,
                       StaffRoleType roleType, String subjects, String qualifications,
                       Integer experienceMinYears, JobType jobType,
                       Integer salaryMin, Integer salaryMax, LocalDate deadline,
                       JobPostingStatus status) {
        this.id = id;
        this.centerId = centerId;
        this.title = title;
        this.description = description;
        this.roleType = roleType;
        this.subjects = subjects;
        this.qualifications = qualifications;
        this.experienceMinYears = experienceMinYears;
        this.jobType = jobType;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.deadline = deadline;
        this.status = status;
        this.postedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Factory method — creates a new job posting.
     *
     * @param initialStatus pass {@link JobPostingStatus#DRAFT} to save without publishing,
     *                      or {@link JobPostingStatus#OPEN} to publish immediately.
     *                      When {@code null}, defaults to {@link JobPostingStatus#OPEN}.
     */
    public static JobPosting create(UUID centerId, String title, String description,
                                    StaffRoleType roleType, String subjects, String qualifications,
                                    Integer experienceMinYears, JobType jobType,
                                    Integer salaryMin, Integer salaryMax, LocalDate deadline,
                                    JobPostingStatus initialStatus) {
        JobPostingStatus effectiveStatus = initialStatus != null ? initialStatus : JobPostingStatus.OPEN;
        return new JobPosting(UUID.randomUUID(), centerId, title, description,
                roleType, subjects, qualifications, experienceMinYears, jobType,
                salaryMin, salaryMax, deadline, effectiveStatus);
    }

    // ─── Domain state transitions ──────────────────────────────────────────────

    /**
     * Transitions the posting from DRAFT to OPEN, making it visible on the job board.
     */
    public void publish() {
        if (this.status != JobPostingStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only a DRAFT posting can be published; current status: " + this.status);
        }
        this.status = JobPostingStatus.OPEN;
        this.updatedAt = Instant.now();
    }

    /**
     * Closes an OPEN posting — it will no longer appear on the job board.
     */
    public void close() {
        if (this.status != JobPostingStatus.OPEN) {
            throw new IllegalStateException(
                    "Only an OPEN posting can be closed; current status: " + this.status);
        }
        this.status = JobPostingStatus.CLOSED;
        this.updatedAt = Instant.now();
    }

    /**
     * Marks the position as filled. Allowed from OPEN status.
     */
    public void markFilled() {
        if (this.status != JobPostingStatus.OPEN) {
            throw new IllegalStateException(
                    "Only an OPEN posting can be marked as filled; current status: " + this.status);
        }
        this.status = JobPostingStatus.FILLED;
        this.updatedAt = Instant.now();
    }

    /**
     * Reverts a CLOSED or FILLED posting back to DRAFT for re-editing and re-publishing.
     */
    public void toDraft() {
        if (this.status != JobPostingStatus.CLOSED && this.status != JobPostingStatus.FILLED) {
            throw new IllegalStateException(
                    "Only CLOSED or FILLED postings can be reverted to DRAFT; current status: " + this.status);
        }
        this.status = JobPostingStatus.DRAFT;
        this.updatedAt = Instant.now();
    }

    /**
     * Updates the editable details of the posting.
     * Only non-null parameters are applied (PATCH semantics).
     */
    public void updateDetails(String title, String description, StaffRoleType roleType,
                              String subjects, String qualifications,
                              Integer experienceMinYears, JobType jobType,
                              Integer salaryMin, Integer salaryMax, LocalDate deadline) {
        if (title               != null) this.title               = title;
        if (description         != null) this.description         = description;
        if (roleType            != null) this.roleType            = roleType;
        if (subjects            != null) this.subjects            = subjects;
        if (qualifications      != null) this.qualifications      = qualifications;
        if (experienceMinYears  != null) this.experienceMinYears  = experienceMinYears;
        if (jobType             != null) this.jobType             = jobType;
        if (salaryMin           != null) this.salaryMin           = salaryMin;
        if (salaryMax           != null) this.salaryMax           = salaryMax;
        if (deadline            != null) this.deadline            = deadline;
        this.updatedAt = Instant.now();
    }

    /**
     * Soft-deletes the posting. Soft-deleted postings are excluded from all queries.
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public UUID getId()                    { return id; }
    public UUID getCenterId()              { return centerId; }
    public String getTitle()               { return title; }
    public String getDescription()         { return description; }
    public StaffRoleType getRoleType()     { return roleType; }
    public String getSubjects()            { return subjects; }
    public String getQualifications()      { return qualifications; }
    public Integer getExperienceMinYears() { return experienceMinYears; }
    public JobType getJobType()            { return jobType; }
    public Integer getSalaryMin()          { return salaryMin; }
    public Integer getSalaryMax()          { return salaryMax; }
    public LocalDate getDeadline()         { return deadline; }
    public JobPostingStatus getStatus()    { return status; }
    public Instant getPostedAt()           { return postedAt; }
    public Instant getUpdatedAt()          { return updatedAt; }
    public Instant getDeletedAt()          { return deletedAt; }
    public Long getVersion()               { return version; }
}
