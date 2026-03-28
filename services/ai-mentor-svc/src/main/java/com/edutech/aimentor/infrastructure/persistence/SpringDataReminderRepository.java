package com.edutech.aimentor.infrastructure.persistence;

import com.edutech.aimentor.domain.model.StudyPlanReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataReminderRepository extends JpaRepository<StudyPlanReminder, UUID> {

    List<StudyPlanReminder> findByStudentIdAndAcknowledgedFalseAndRemindAtBefore(
            UUID studentId, Instant now);

    List<StudyPlanReminder> findByStudyPlanIdAndStudentId(UUID studyPlanId, UUID studentId);
}
