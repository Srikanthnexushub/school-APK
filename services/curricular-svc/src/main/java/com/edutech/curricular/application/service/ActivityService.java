package com.edutech.curricular.application.service;

import com.edutech.curricular.application.dto.AchievementCreateRequest;
import com.edutech.curricular.application.dto.ActivityCreateRequest;
import com.edutech.curricular.application.dto.ActivitySessionCreateRequest;
import com.edutech.curricular.domain.model.*;
import com.edutech.curricular.domain.model.enums.*;
import com.edutech.curricular.domain.port.in.ManageActivityUseCase;
import com.edutech.curricular.domain.port.in.QueryActivityUseCase;
import com.edutech.curricular.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService implements ManageActivityUseCase, QueryActivityUseCase {

    private final ActivityRepository activityRepository;
    private final ActivityEnrollmentRepository enrollmentRepository;
    private final ActivitySessionRepository sessionRepository;
    private final ActivityAchievementRepository achievementRepository;
    private final CurricularEventPublisher eventPublisher;

    @Override
    @Transactional
    public ExtracurricularActivity createActivity(UUID centerId, ActivityCreateRequest request, UUID createdBy) {
        ActivityCategory category = ActivityCategory.valueOf(request.category());
        ExtracurricularActivity activity = ExtracurricularActivity.create(
            centerId, request.name(), request.description(), category,
            request.maxParticipants(), request.scheduleDescription(), createdBy
        );
        return activityRepository.save(activity);
    }

    @Override
    @Transactional
    public ActivityEnrollment enrollStudent(UUID activityId, UUID studentId) {
        ExtracurricularActivity activity = activityRepository.findById(activityId)
            .orElseThrow(() -> new NoSuchElementException("Activity not found: " + activityId));

        // Check if already enrolled
        enrollmentRepository.findByActivityIdAndStudentId(activityId, studentId)
            .ifPresent(existing -> {
                if (existing.getStatus() != EnrollmentStatus.WITHDRAWN) {
                    throw new DuplicateEnrollmentException(
                        "Student " + studentId + " is already enrolled in activity " + activityId
                    );
                }
            });

        // Check capacity
        long activeCount = enrollmentRepository.countActiveByActivityId(activityId);
        EnrollmentStatus status = activeCount >= activity.getMaxParticipants()
            ? EnrollmentStatus.WAITLISTED
            : EnrollmentStatus.ENROLLED;

        ActivityEnrollment enrollment = ActivityEnrollment.create(activityId, studentId, status);
        ActivityEnrollment saved = enrollmentRepository.save(enrollment);

        if (status == EnrollmentStatus.ENROLLED) {
            eventPublisher.publishActivityEnrolled(studentId, activityId);
        }
        return saved;
    }

    @Override
    @Transactional
    public ActivityEnrollment withdrawStudent(UUID activityId, UUID studentId) {
        ActivityEnrollment enrollment = enrollmentRepository
            .findByActivityIdAndStudentId(activityId, studentId)
            .orElseThrow(() -> new NoSuchElementException(
                "Enrollment not found for student " + studentId + " in activity " + activityId
            ));
        enrollment.withdraw();
        return enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional
    public ActivitySession addSession(UUID activityId, ActivitySessionCreateRequest request, UUID conductedBy) {
        activityRepository.findById(activityId)
            .orElseThrow(() -> new NoSuchElementException("Activity not found: " + activityId));

        ActivitySession session = ActivitySession.create(
            activityId, request.sessionDate(), request.venue(),
            request.durationMins(), conductedBy, request.notes()
        );
        return sessionRepository.save(session);
    }

    @Override
    @Transactional
    public ActivityAchievement awardAchievement(UUID activityId, UUID studentId, AchievementCreateRequest request, UUID issuedBy) {
        activityRepository.findById(activityId)
            .orElseThrow(() -> new NoSuchElementException("Activity not found: " + activityId));

        AchievementType achievementType = AchievementType.valueOf(request.achievementType());
        ActivityAchievement achievement = ActivityAchievement.create(
            studentId, activityId, achievementType,
            request.title(), request.description(), request.issuedAt(), issuedBy
        );
        ActivityAchievement saved = achievementRepository.save(achievement);
        eventPublisher.publishAchievementAwarded(studentId, activityId, request.achievementType());
        return saved;
    }

    @Override
    @Transactional
    public void deactivateActivity(UUID activityId, UUID requestedBy) {
        ExtracurricularActivity activity = activityRepository.findById(activityId)
            .orElseThrow(() -> new NoSuchElementException("Activity not found: " + activityId));
        activity.deactivate();
        activityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtracurricularActivity> listActivitiesByCenter(UUID centerId, ActivityCategory category) {
        if (category != null) {
            return activityRepository.findByCenterIdAndCategoryAndActive(centerId, category, true);
        }
        return activityRepository.findByCenterIdAndActive(centerId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityEnrollment> getStudentEnrollments(UUID studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivitySession> getActivitySessions(UUID activityId) {
        return sessionRepository.findByActivityIdOrderBySessionDateAsc(activityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityAchievement> getStudentAchievements(UUID studentId) {
        return achievementRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityAchievement> getChildActivitiesAndAchievements(UUID studentId) {
        return achievementRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityEnrollment> getActivityEnrollments(UUID activityId) {
        return enrollmentRepository.findByActivityId(activityId).stream()
            .filter(e -> e.getStatus() != EnrollmentStatus.WITHDRAWN)
            .toList();
    }
}
