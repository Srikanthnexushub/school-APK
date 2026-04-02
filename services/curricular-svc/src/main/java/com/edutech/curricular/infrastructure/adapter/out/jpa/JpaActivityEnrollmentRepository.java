package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.ActivityEnrollment;
import com.edutech.curricular.domain.model.enums.EnrollmentStatus;
import com.edutech.curricular.domain.port.out.ActivityEnrollmentRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaActivityEnrollmentRepository extends JpaRepository<ActivityEnrollment, UUID>, ActivityEnrollmentRepository {

    @Override
    List<ActivityEnrollment> findByActivityId(UUID activityId);

    @Override
    List<ActivityEnrollment> findByStudentId(UUID studentId);

    @Override
    Optional<ActivityEnrollment> findByActivityIdAndStudentId(UUID activityId, UUID studentId);

    @Query("SELECT COUNT(ae) FROM ActivityEnrollment ae WHERE ae.activityId = :activityId AND ae.status IN :statuses")
    long countByActivityIdAndStatusIn(@Param("activityId") UUID activityId, @Param("statuses") List<EnrollmentStatus> statuses);

    @Override
    default long countActiveByActivityId(UUID activityId) {
        return countByActivityIdAndStatusIn(activityId,
            List.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.WAITLISTED));
    }
}
