package com.edutech.aimentor.domain.port.in;

import com.edutech.aimentor.application.dto.StudyPlanResponse;

import java.util.UUID;

public interface GetStudyPlanUseCase {

    StudyPlanResponse getStudyPlan(UUID studentId, UUID enrollmentId);

    StudyPlanResponse getStudyPlanById(UUID planId, UUID studentId);
}
