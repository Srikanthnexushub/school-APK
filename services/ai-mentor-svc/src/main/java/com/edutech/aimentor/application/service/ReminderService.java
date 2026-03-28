package com.edutech.aimentor.application.service;

import com.edutech.aimentor.application.dto.ReminderRequest;
import com.edutech.aimentor.application.dto.ReminderResponse;
import com.edutech.aimentor.domain.model.StudyPlanReminder;
import com.edutech.aimentor.infrastructure.persistence.SpringDataReminderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReminderService {

    private final SpringDataReminderRepository repo;

    public ReminderService(SpringDataReminderRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ReminderResponse createReminder(UUID studentId, ReminderRequest request) {
        StudyPlanReminder reminder = StudyPlanReminder.create(
                request.studyPlanId(),
                studentId,
                request.reminderType(),
                request.remindAt(),
                request.title(),
                request.message()
        );
        return toResponse(repo.save(reminder));
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> getPendingReminders(UUID studentId) {
        return repo.findByStudentIdAndAcknowledgedFalseAndRemindAtBefore(studentId, Instant.now())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> getRemindersForPlan(UUID studyPlanId, UUID studentId) {
        return repo.findByStudyPlanIdAndStudentId(studyPlanId, studentId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReminderResponse acknowledge(UUID reminderId, UUID studentId) {
        StudyPlanReminder reminder = repo.findById(reminderId)
                .filter(r -> r.getStudentId().equals(studentId))
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found: " + reminderId));
        reminder.acknowledge();
        return toResponse(repo.save(reminder));
    }

    private ReminderResponse toResponse(StudyPlanReminder r) {
        return new ReminderResponse(r.getId(), r.getStudyPlanId(), r.getStudentId(),
                r.getReminderType(), r.getRemindAt(), r.getTitle(), r.getMessage(),
                r.isAcknowledged(), r.getAcknowledgedAt(), r.getCreatedAt());
    }
}
