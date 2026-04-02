package com.edutech.curricular.domain.port.in;

import com.edutech.curricular.domain.model.LearningPath;

import java.util.UUID;

public interface ComputeLearningPathUseCase {
    LearningPath computeOrRefresh(UUID studentId, UUID subjectId, UUID gradeId, String jwt);
}
