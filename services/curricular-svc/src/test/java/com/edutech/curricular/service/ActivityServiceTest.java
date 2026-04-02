package com.edutech.curricular.service;

import com.edutech.curricular.application.dto.AchievementCreateRequest;
import com.edutech.curricular.application.service.ActivityService;
import com.edutech.curricular.application.service.DuplicateEnrollmentException;
import com.edutech.curricular.domain.model.ActivityAchievement;
import com.edutech.curricular.domain.model.ActivityEnrollment;
import com.edutech.curricular.domain.model.ExtracurricularActivity;
import com.edutech.curricular.domain.model.enums.ActivityCategory;
import com.edutech.curricular.domain.model.enums.EnrollmentStatus;
import com.edutech.curricular.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityService tests")
class ActivityServiceTest {

    @Mock private ActivityRepository activityRepository;
    @Mock private ActivityEnrollmentRepository enrollmentRepository;
    @Mock private ActivitySessionRepository sessionRepository;
    @Mock private ActivityAchievementRepository achievementRepository;
    @Mock private CurricularEventPublisher eventPublisher;

    private ActivityService service;

    // Static immutable fixtures
    static final UUID CENTER_ID   = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    static final UUID STUDENT_ID  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID ACTIVITY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    static final UUID TEACHER_ID  = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    static ExtracurricularActivity buildActivity(int maxParticipants) {
        ExtracurricularActivity a = ExtracurricularActivity.create(
            CENTER_ID, "Chess Club", "Learn chess", ActivityCategory.OTHER,
            maxParticipants, "Monday evenings", TEACHER_ID
        );
        a.setId(ACTIVITY_ID);
        return a;
    }

    @BeforeEach
    void setUp() {
        service = new ActivityService(
            activityRepository, enrollmentRepository,
            sessionRepository, achievementRepository, eventPublisher
        );
    }

    @Test
    @DisplayName("enrollStudent: no existing enrollment + space available -> ENROLLED")
    void enroll_student_when_space_available() {
        ExtracurricularActivity activity = buildActivity(30);
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(enrollmentRepository.findByActivityIdAndStudentId(ACTIVITY_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(enrollmentRepository.countActiveByActivityId(ACTIVITY_ID)).thenReturn(5L);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ActivityEnrollment enrollment = service.enrollStudent(ACTIVITY_ID, STUDENT_ID);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        verify(eventPublisher).publishActivityEnrolled(STUDENT_ID, ACTIVITY_ID);
    }

    @Test
    @DisplayName("enrollStudent: at capacity -> WAITLISTED")
    void enroll_student_when_at_capacity() {
        ExtracurricularActivity activity = buildActivity(5);
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(enrollmentRepository.findByActivityIdAndStudentId(ACTIVITY_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(enrollmentRepository.countActiveByActivityId(ACTIVITY_ID)).thenReturn(5L);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ActivityEnrollment enrollment = service.enrollStudent(ACTIVITY_ID, STUDENT_ID);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
        verify(eventPublisher, never()).publishActivityEnrolled(any(), any());
    }

    @Test
    @DisplayName("enrollStudent: already enrolled -> throws DuplicateEnrollmentException")
    void enroll_student_already_enrolled_throws() {
        ExtracurricularActivity activity = buildActivity(30);
        ActivityEnrollment existing = ActivityEnrollment.create(ACTIVITY_ID, STUDENT_ID, EnrollmentStatus.ENROLLED);

        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(enrollmentRepository.findByActivityIdAndStudentId(ACTIVITY_ID, STUDENT_ID))
            .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.enrollStudent(ACTIVITY_ID, STUDENT_ID))
            .isInstanceOf(DuplicateEnrollmentException.class);
    }

    @Test
    @DisplayName("withdrawStudent: enrolled -> WITHDRAWN")
    void withdraw_student_sets_withdrawn_status() {
        ActivityEnrollment enrollment = ActivityEnrollment.create(ACTIVITY_ID, STUDENT_ID, EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByActivityIdAndStudentId(ACTIVITY_ID, STUDENT_ID))
            .thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ActivityEnrollment result = service.withdrawStudent(ACTIVITY_ID, STUDENT_ID);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.WITHDRAWN);
        assertThat(result.getWithdrawnAt()).isNotNull();
    }

    @Test
    @DisplayName("awardAchievement: valid activity and student -> creates achievement and publishes event")
    void award_achievement_creates_and_publishes() {
        ExtracurricularActivity activity = buildActivity(30);
        AchievementCreateRequest request = new AchievementCreateRequest(
            "GOLD", "Chess Champion", "Won the tournament", LocalDate.now()
        );

        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(achievementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ActivityAchievement result = service.awardAchievement(ACTIVITY_ID, STUDENT_ID, request, TEACHER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getStudentId()).isEqualTo(STUDENT_ID);
        assertThat(result.getActivityId()).isEqualTo(ACTIVITY_ID);
        verify(eventPublisher).publishAchievementAwarded(STUDENT_ID, ACTIVITY_ID, "GOLD");
    }
}
