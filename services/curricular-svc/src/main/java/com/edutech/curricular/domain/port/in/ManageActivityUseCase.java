package com.edutech.curricular.domain.port.in;

import com.edutech.curricular.application.dto.AchievementCreateRequest;
import com.edutech.curricular.application.dto.ActivityCreateRequest;
import com.edutech.curricular.application.dto.ActivitySessionCreateRequest;
import com.edutech.curricular.domain.model.ActivityAchievement;
import com.edutech.curricular.domain.model.ActivityEnrollment;
import com.edutech.curricular.domain.model.ActivitySession;
import com.edutech.curricular.domain.model.ExtracurricularActivity;

import java.util.UUID;

public interface ManageActivityUseCase {
    ExtracurricularActivity createActivity(UUID centerId, ActivityCreateRequest request, UUID createdBy);
    ActivityEnrollment enrollStudent(UUID activityId, UUID studentId);
    ActivityEnrollment withdrawStudent(UUID activityId, UUID studentId);
    ActivitySession addSession(UUID activityId, ActivitySessionCreateRequest request, UUID conductedBy);
    ActivityAchievement awardAchievement(UUID activityId, UUID studentId, AchievementCreateRequest request, UUID issuedBy);
    void deactivateActivity(UUID activityId, UUID requestedBy);
}
