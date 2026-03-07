package com.edutech.aimentor.infrastructure.persistence;

import com.edutech.aimentor.domain.model.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

interface SpringDataStudyPlanRepository extends JpaRepository<StudyPlan, UUID> {

    Optional<StudyPlan> findByStudentIdAndEnrollmentIdAndDeletedAtIsNull(UUID studentId, UUID enrollmentId);

    List<StudyPlan> findAllByStudentIdAndDeletedAtIsNull(UUID studentId);
}
