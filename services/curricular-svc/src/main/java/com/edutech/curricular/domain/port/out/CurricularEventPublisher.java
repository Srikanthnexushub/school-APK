package com.edutech.curricular.domain.port.out;

import java.util.UUID;

public interface CurricularEventPublisher {
    void publishPathComputed(UUID studentId, UUID pathId, UUID subjectId);
    void publishPathItemCompleted(UUID studentId, UUID itemId, UUID topicId);
    void publishCoverageLogged(UUID teacherId, UUID centerId, UUID topicId);
    void publishActivityEnrolled(UUID studentId, UUID activityId);
    void publishAchievementAwarded(UUID studentId, UUID activityId, String achievementType);
}
