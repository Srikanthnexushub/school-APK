package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.ActivitySession;

import java.util.List;
import java.util.UUID;

public interface ActivitySessionRepository {
    ActivitySession save(ActivitySession session);
    List<ActivitySession> findByActivityId(UUID activityId);
    List<ActivitySession> findByActivityIdOrderBySessionDateAsc(UUID activityId);
}
