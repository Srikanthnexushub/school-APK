package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.ActivityAchievement;

import java.util.List;
import java.util.UUID;

public interface ActivityAchievementRepository {
    ActivityAchievement save(ActivityAchievement achievement);
    List<ActivityAchievement> findByStudentId(UUID studentId);
    List<ActivityAchievement> findByActivityIdAndStudentId(UUID activityId, UUID studentId);
}
