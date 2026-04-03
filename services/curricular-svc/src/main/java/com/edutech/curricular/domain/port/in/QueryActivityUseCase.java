package com.edutech.curricular.domain.port.in;

import com.edutech.curricular.domain.model.ActivityAchievement;
import com.edutech.curricular.domain.model.ActivityEnrollment;
import com.edutech.curricular.domain.model.ActivitySession;
import com.edutech.curricular.domain.model.ExtracurricularActivity;
import com.edutech.curricular.domain.model.enums.ActivityCategory;

import java.util.List;
import java.util.UUID;

public interface QueryActivityUseCase {
    List<ExtracurricularActivity> listActivitiesByCenter(UUID centerId, ActivityCategory category);
    List<ActivityEnrollment> getStudentEnrollments(UUID studentId);
    List<ActivitySession> getActivitySessions(UUID activityId);
    List<ActivityAchievement> getStudentAchievements(UUID studentId);
    List<ActivityAchievement> getChildActivitiesAndAchievements(UUID studentId);
    List<ActivityEnrollment> getActivityEnrollments(UUID activityId);
}
