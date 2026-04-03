package com.edutech.curricular.infrastructure.adapter.in.web;

import com.edutech.curricular.application.dto.*;
import com.edutech.curricular.application.service.ChildJourneyService;
import com.edutech.curricular.domain.model.*;
import com.edutech.curricular.domain.model.enums.ActivityCategory;
import com.edutech.curricular.domain.port.in.ManageActivityUseCase;
import com.edutech.curricular.domain.port.in.QueryActivityUseCase;
import com.edutech.curricular.domain.port.out.ActivityEnrollmentRepository;
import com.edutech.curricular.domain.port.out.ActivityRepository;
import com.edutech.curricular.infrastructure.config.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/curricular")
@RequiredArgsConstructor
public class ActivityController {

    private final ManageActivityUseCase manageActivityUseCase;
    private final QueryActivityUseCase queryActivityUseCase;
    private final ActivityRepository activityRepository;
    private final ActivityEnrollmentRepository enrollmentRepository;
    private final ChildJourneyService childJourneyService;

    @GetMapping("/activities")
    public ResponseEntity<List<ActivityResponse>> listActivities(
            @RequestParam UUID centerId,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal AuthPrincipal principal) {
        ActivityCategory activityCategory = category != null ? ActivityCategory.valueOf(category) : null;
        List<ActivityResponse> responses = queryActivityUseCase.listActivitiesByCenter(centerId, activityCategory).stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }

    private static final java.util.Set<String> ADMIN_ROLES =
            java.util.Set.of("CENTER_ADMIN", "INSTITUTION_ADMIN", "SUPER_ADMIN");

    @PostMapping("/activities")
    public ResponseEntity<?> createActivity(
            @RequestBody ActivityCreateRequest request,
            @RequestParam UUID centerId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!ADMIN_ROLES.contains(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ExtracurricularActivity activity = manageActivityUseCase.createActivity(centerId, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(activity));
    }

    @DeleteMapping("/activities/{id}")
    public ResponseEntity<Void> deactivateActivity(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!ADMIN_ROLES.contains(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        manageActivityUseCase.deactivateActivity(id, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/activities/{id}/enroll")
    public ResponseEntity<?> enrollStudent(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!"STUDENT".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ActivityEnrollment enrollment = manageActivityUseCase.enrollStudent(id, principal.userId());
        String activityName = activityRepository.findById(id).map(ExtracurricularActivity::getName).orElse("Unknown");
        String category = activityRepository.findById(id).map(a -> a.getCategory().name()).orElse("OTHER");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ActivityEnrollmentResponse(enrollment.getId(), enrollment.getActivityId(), activityName, category, enrollment.getStatus().name(), enrollment.getEnrolledAt()));
    }

    @DeleteMapping("/activities/{id}/enroll")
    public ResponseEntity<Void> withdrawStudent(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!"STUDENT".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        manageActivityUseCase.withdrawStudent(id, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-activities")
    public ResponseEntity<?> getMyActivities(@AuthenticationPrincipal AuthPrincipal principal) {
        if (!"STUDENT".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<ActivityEnrollmentResponse> responses = queryActivityUseCase.getStudentEnrollments(principal.userId()).stream()
            .map(enrollment -> {
                String activityName = activityRepository.findById(enrollment.getActivityId())
                    .map(ExtracurricularActivity::getName).orElse("Unknown");
                String category = activityRepository.findById(enrollment.getActivityId())
                    .map(a -> a.getCategory().name()).orElse("OTHER");
                return new ActivityEnrollmentResponse(
                    enrollment.getId(), enrollment.getActivityId(), activityName, category,
                    enrollment.getStatus().name(), enrollment.getEnrolledAt()
                );
            }).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/activities/{id}/sessions")
    public ResponseEntity<List<ActivitySessionResponse>> getActivitySessions(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<ActivitySessionResponse> responses = queryActivityUseCase.getActivitySessions(id).stream()
            .map(s -> new ActivitySessionResponse(s.getId(), s.getActivityId(), s.getSessionDate(), s.getVenue(), s.getDurationMins(), s.getNotes()))
            .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/activities/{id}/sessions")
    public ResponseEntity<?> addSession(
            @PathVariable UUID id,
            @RequestBody ActivitySessionCreateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!"TEACHER".equals(principal.role()) && !"CENTER_ADMIN".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ActivitySession session = manageActivityUseCase.addSession(id, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ActivitySessionResponse(session.getId(), session.getActivityId(), session.getSessionDate(), session.getVenue(), session.getDurationMins(), session.getNotes()));
    }

    @PostMapping("/activities/{id}/achievements")
    public ResponseEntity<?> awardAchievement(
            @PathVariable UUID id,
            @RequestParam UUID studentId,
            @RequestBody AchievementCreateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!"TEACHER".equals(principal.role()) && !"CENTER_ADMIN".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ActivityAchievement achievement = manageActivityUseCase.awardAchievement(id, studentId, request, principal.userId());
        String activityName = activityRepository.findById(id).map(ExtracurricularActivity::getName).orElse("Unknown");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new AchievementResponse(achievement.getId(), achievement.getStudentId(), achievement.getActivityId(), activityName, achievement.getAchievementType().name(), achievement.getTitle(), achievement.getIssuedAt()));
    }

    @GetMapping("/my-achievements")
    public ResponseEntity<?> getMyAchievements(@AuthenticationPrincipal AuthPrincipal principal) {
        if (!"STUDENT".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<AchievementResponse> responses = queryActivityUseCase.getStudentAchievements(principal.userId()).stream()
            .map(achievement -> {
                String activityName = activityRepository.findById(achievement.getActivityId())
                    .map(ExtracurricularActivity::getName).orElse("Unknown");
                return new AchievementResponse(achievement.getId(), achievement.getStudentId(), achievement.getActivityId(), activityName, achievement.getAchievementType().name(), achievement.getTitle(), achievement.getIssuedAt());
            }).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/child/{studentId}/activities")
    public ResponseEntity<?> getChildActivities(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!"PARENT".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ChildJourneyResponse journey = childJourneyService.getChildJourney(studentId, principal.userId());
        return ResponseEntity.ok(journey.activities());
    }

    @GetMapping("/child/{studentId}/journey")
    public ResponseEntity<?> getChildJourney(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!"PARENT".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ChildJourneyResponse journey = childJourneyService.getChildJourney(studentId, principal.userId());
        return ResponseEntity.ok(journey);
    }

    @GetMapping("/activities/{id}/enrollments")
    public ResponseEntity<List<ActivityEnrollmentAdminResponse>> getActivityEnrollments(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!ADMIN_ROLES.contains(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<ActivityEnrollmentAdminResponse> responses = queryActivityUseCase.getActivityEnrollments(id).stream()
            .map(e -> new ActivityEnrollmentAdminResponse(e.getId(), e.getStudentId(), e.getStatus().name(), e.getEnrolledAt()))
            .toList();
        return ResponseEntity.ok(responses);
    }

    private ActivityResponse toResponse(ExtracurricularActivity activity) {
        int enrollmentCount = (int) enrollmentRepository.countActiveByActivityId(activity.getId());
        return new ActivityResponse(
            activity.getId(), activity.getCenterId(), activity.getName(),
            activity.getDescription(), activity.getCategory().name(),
            activity.getMaxParticipants(), activity.getScheduleDescription(), activity.isActive(),
            enrollmentCount
        );
    }
}
