package com.edutech.aimentor.domain.port.in;

import com.edutech.aimentor.application.dto.StudyPlanItemResponse;

import java.util.UUID;

public interface UpdateStudyPlanItemUseCase {

    StudyPlanItemResponse reviewItem(UUID itemId, UUID studentId, int quality);
}
