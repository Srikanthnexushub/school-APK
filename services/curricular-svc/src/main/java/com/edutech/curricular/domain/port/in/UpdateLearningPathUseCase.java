package com.edutech.curricular.domain.port.in;

import com.edutech.curricular.domain.model.LearningPathItem;
import com.edutech.curricular.domain.model.enums.PathItemStatus;

import java.util.UUID;

public interface UpdateLearningPathUseCase {
    LearningPathItem updateItemStatus(UUID itemId, UUID studentId, PathItemStatus newStatus);
}
