// src/main/java/com/edutech/center/infrastructure/persistence/JobPostingPersistenceAdapter.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.JobPosting;
import com.edutech.center.domain.model.JobType;
import com.edutech.center.domain.model.StaffRoleType;
import com.edutech.center.domain.port.out.JobPostingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter that bridges the domain {@link JobPostingRepository} port
 * and the Spring Data JPA {@link SpringDataJobPostingRepository}.
 */
@Component
public class JobPostingPersistenceAdapter implements JobPostingRepository {

    private final SpringDataJobPostingRepository jpa;

    public JobPostingPersistenceAdapter(SpringDataJobPostingRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<JobPosting> findByIdAndCenterId(UUID id, UUID centerId) {
        return jpa.findByIdAndCenterIdActive(id, centerId);
    }

    @Override
    public Optional<JobPosting> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<JobPosting> findByCenterId(UUID centerId) {
        return jpa.findByCenterIdActive(centerId);
    }

    @Override
    public Page<JobPosting> findAllOpen(StaffRoleType roleType, JobType jobType,
                                        String city, Pageable pageable) {
        return jpa.findAllOpen(roleType, jobType, city, pageable);
    }

    @Override
    public JobPosting save(JobPosting job) {
        return jpa.save(job);
    }
}
