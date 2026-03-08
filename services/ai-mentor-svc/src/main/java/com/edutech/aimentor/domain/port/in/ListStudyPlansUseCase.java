package com.edutech.aimentor.domain.port.in;

import com.edutech.aimentor.application.dto.StudyPlanResponse;

import java.util.List;
import java.util.UUID;

public interface ListStudyPlansUseCase {

    List<StudyPlanResponse> listStudyPlans(UUID studentId);
}
