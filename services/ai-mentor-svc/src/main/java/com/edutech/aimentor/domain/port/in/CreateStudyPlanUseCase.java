package com.edutech.aimentor.domain.port.in;

import com.edutech.aimentor.application.dto.CreateStudyPlanRequest;
import com.edutech.aimentor.application.dto.StudyPlanResponse;

public interface CreateStudyPlanUseCase {

    StudyPlanResponse createStudyPlan(CreateStudyPlanRequest request);
}
