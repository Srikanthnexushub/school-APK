package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.ActivityEnrollment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityEnrollmentRepository {
    ActivityEnrollment save(ActivityEnrollment enrollment);
    Optional<ActivityEnrollment> findById(UUID id);
    List<ActivityEnrollment> findByActivityId(UUID activityId);
    List<ActivityEnrollment> findByStudentId(UUID studentId);
    Optional<ActivityEnrollment> findByActivityIdAndStudentId(UUID activityId, UUID studentId);
    long countActiveByActivityId(UUID activityId);
}
