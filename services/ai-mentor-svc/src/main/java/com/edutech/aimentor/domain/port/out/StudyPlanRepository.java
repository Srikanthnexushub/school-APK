package com.edutech.aimentor.domain.port.out;

import com.edutech.aimentor.domain.model.StudyPlan;
import com.edutech.aimentor.domain.model.StudyPlanItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudyPlanRepository {

    StudyPlan save(StudyPlan studyPlan);

    Optional<StudyPlan> findByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId);

    Optional<StudyPlan> findById(UUID id);

    Optional<StudyPlanItem> findItemById(UUID itemId);

    StudyPlanItem saveItem(StudyPlanItem item);

    List<StudyPlan> findAllByStudentId(UUID studentId);
}
